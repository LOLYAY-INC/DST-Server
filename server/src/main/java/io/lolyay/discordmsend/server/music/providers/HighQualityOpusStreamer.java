package io.lolyay.discordmsend.server.music.providers;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import io.github.jaredmdobson.concentus.OpusApplication;
import io.github.jaredmdobson.concentus.OpusEncoder;
import io.github.jaredmdobson.concentus.OpusSignal;
import io.lolyay.discordmsend.obj.EndReason;
import io.lolyay.discordmsend.obj.Severity;
import io.lolyay.discordmsend.server.cache.AudioCacheManager;
import io.lolyay.discordmsend.server.config.ConfigFile;
import io.lolyay.discordmsend.server.music.players.TrackPlayerInstance;
import io.lolyay.discordmsend.util.logging.Logger;

import java.io.*;
import java.util.concurrent.*;

/**
 * High-quality Opus audio streamer using yt-dlp + ffmpeg
 * Implements MediaFrameProvider for KOE compatibility
 */
public class HighQualityOpusStreamer {
    private final BlockingQueue<short[]> pcmBuffer = new LinkedBlockingQueue<>(15_000); // ~5 minutes of PCM audio buffer
    private final BlockingQueue<byte[]> opusBuffer = new LinkedBlockingQueue<>(750); // 15 seconds of Opus frames
    private final int frameSize = 960; // 20ms @ 48kHz
    private final OpusEncoder encoder;
    private final AudioCacheManager cacheManager;
    private Process ytDlpProcess;
    private Process ffmpegProcess;
    private volatile float volume = 1.0f;
    private volatile boolean paused = false;
    private volatile boolean playing = false;
    private volatile boolean buffering = false;
    private volatile boolean streamEnded = false;
    private volatile boolean loadingFromCache = false;
    private Thread pipeThread;
    private Thread decodeThread;
    private Thread encoderThread;
    private Thread cacheThread;
    private AudioTrack playingTrack = null;
    private String currentTrackUri = null;
    private OutputStream cacheOutputStream = null;
    private final TrackPlayerInstance player;
    
    private static final int MIN_BUFFER_SIZE = 250; // 5 seconds
    private static final int REBUFFER_THRESHOLD = 100; // 2 seconds
    private static final int OPUS_BUFFER_TARGET = 750; // 15 seconds
    private static final int OPUS_BUFFER_LOW = 200; // 4 seconds
    private static final int PCM_BUFFER_HIGH = 10_000; // 200 seconds
    private static final int PCM_BUFFER_CACHE_TARGET = 600; // 12 seconds
    private static final int BUFFER_PUT_TIMEOUT_MS = 100;

    public HighQualityOpusStreamer(TrackPlayerInstance callback, AudioCacheManager cacheManager) throws Exception {
        this.player = callback;
        this.cacheManager = cacheManager;
        encoder = new OpusEncoder(48000, 2, OpusApplication.OPUS_APPLICATION_AUDIO);
        encoder.setBitrate(384_000);
        encoder.setSignalType(OpusSignal.OPUS_SIGNAL_MUSIC);
        encoder.setComplexity(10);
        volume = callback.getDefaultVolume();
        Logger.debug("HQ Opus Streamer initialized with volume: " + volume);
    }


    private File getExecutable(String baseName) {
        try {
            // Get the directory containing the JAR
            File jarDir = new File(HighQualityOpusStreamer.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI())
                    .getParentFile();

            // Try different executable names for different platforms
            String osName = System.getProperty("os.name").toLowerCase();
            boolean isWindows = osName.contains("win");
            String execName = isWindows ? baseName + ".exe" : baseName;
            
            // Check in JAR directory
            File exec = new File(jarDir, execName);
            if (exec.exists()) {
                if (!isWindows && !exec.canExecute()) {
                    // Try to make it executable
                    if (!exec.setExecutable(true)) {
                        Logger.warn("Could not set executable permission for: " + exec.getAbsolutePath());
                    }
                }
                if (isWindows || exec.canExecute()) {
                    return exec.getAbsoluteFile();
                }
            }
            
            // Check in bin/ subdirectory
            exec = new File(jarDir, "bin/" + execName);
            if (exec.exists()) {
                if (!isWindows && !exec.canExecute()) {
                    // Try to make it executable
                    if (!exec.setExecutable(true)) {
                        Logger.warn("Could not set executable permission for: " + exec.getAbsolutePath());
                    }
                }
                if (isWindows || exec.canExecute()) {
                    return exec.getAbsoluteFile();
                }
            }
            
            // Fall back to system PATH
            File pathExec = new File(execName);
            if (pathExec.exists() && !isWindows && !pathExec.canExecute()) {
                if (!pathExec.setExecutable(true)) {
                    Logger.warn("Could not set executable permission for: " + pathExec.getAbsolutePath());
                }
            }
            return pathExec.getAbsoluteFile();
            
        } catch (Exception e) {
            Logger.err("Error locating " + baseName + ": " + e.getMessage());
            return new File(baseName).getAbsoluteFile();
        }
    }

    private File getYtDlpExecutable() {
        return getExecutable("yt-dlp");
    }

    private File getFfmpegExecutable() {
        return getExecutable("ffmpeg");
    }
    
    private void loadFromCache(String trackUri) throws IOException {
        Logger.info("Loading track from cache: " + trackUri);
        
        // Ensure clean state - stop any existing playback first
        stopPipeline();
        
        // Give threads time to fully terminate
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Clear buffers BEFORE resetting state
        opusBuffer.clear();
        pcmBuffer.clear();
        Logger.debug("Buffers cleared for cache loading. PCM: " + pcmBuffer.size() + ", Opus: " + opusBuffer.size());
        
        // Reset state
        loadingFromCache = true;
        playing = true;
        buffering = true;
        streamEnded = false;
        paused = false;
        
        // Reset encoder state
        encoder.resetState();
        Logger.debug("Encoder state reset for cache loading");
        
        // Start encoder thread FIRST to ensure it's ready to process data
        encoderThread = new Thread(this::encodeToOpus, "Opus-Encoder");
        encoderThread.start();
        
        // Give encoder thread a moment to initialize
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Start decode thread to read from cache
        decodeThread = new Thread(() -> decodePcmFromCache(trackUri), "Cache-PCM-Decoder");
        decodeThread.start();
        
        // Set priorities: Encoder > Decoder (encoder is more time-critical for preventing stuttering)
        // For cache loading, prioritize encoder even more since disk I/O is very fast
        decodeThread.setPriority(Thread.NORM_PRIORITY + 1); // Lower priority for cache decoder
        encoderThread.setPriority(Thread.NORM_PRIORITY + 3); // High priority for encoder
    }
    
    private void decodePcmFromCache(String trackUri) {
        try (InputStream cacheIn = cacheManager.loadTrack(trackUri)) {
            byte[] byteBuffer = new byte[frameSize * 2 * 2]; // 16-bit * 2 channels
            int bytesRead;
            int frameCount = 0;
            
            while (playing) {
                // Wait if paused
                while (paused && playing) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                
                if (!playing) break;
                
                // CRITICAL: Check buffer size BEFORE reading from cache
                // For cache loading, use aggressive back-pressure to prevent buffer overflow
                // Cache disk reads are MUCH faster than real-time, so we need to throttle heavily
                while (pcmBuffer.size() > PCM_BUFFER_CACHE_TARGET && playing && !paused) {
                    try {
                        Thread.sleep(50); // Aggressive back-pressure for cache loading
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                
                // Additional rate limiting: wait if encoder is falling behind
                // This ensures we don't read from cache faster than encoder can process
                while (pcmBuffer.size() > MIN_BUFFER_SIZE * 2 && playing && !paused) {
                    try {
                        Thread.sleep(20); // Delay to let encoder catch up
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                
                // NOW read from cache after ensuring buffer has space
                bytesRead = cacheIn.readNBytes(byteBuffer, 0, byteBuffer.length);
                if (bytesRead <= 0) {
                    break; // End of cache file
                }
                
                // If we got less than a full frame, pad with zeros
                if (bytesRead < byteBuffer.length) {
                    for (int i = bytesRead; i < byteBuffer.length; i++) {
                        byteBuffer[i] = 0;
                    }
                }
                
                // Convert bytes to shorts
                short[] samples = new short[frameSize * 2]; // stereo
                for (int i = 0; i < frameSize * 2; i++) {
                    int lo = byteBuffer[i * 2] & 0xFF;
                    int hi = byteBuffer[i * 2 + 1];
                    samples[i] = (short) ((hi << 8) | lo);
                }
                
                // Store raw PCM samples (with timeout to prevent blocking)
                if (!pcmBuffer.offer(samples, BUFFER_PUT_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    Logger.warn("PCM buffer full, dropping frame to prevent blocking (cache). Buffer size: " + pcmBuffer.size());
                }
                frameCount++;
                
                // Log progress periodically
                if (frameCount % 1000 == 0) {
                    Logger.debug("Cache loading progress: " + frameCount + " frames, PCM buffer: " + pcmBuffer.size() + ", Opus buffer: " + opusBuffer.size());
                }
                
                // Initial buffering
                if (buffering && pcmBuffer.size() >= MIN_BUFFER_SIZE) {
                    buffering = false;
                    Logger.info("Cache buffering complete (" + pcmBuffer.size() + " frames)");
                    player.onTrackStart(playingTrack);
                }
            }
            
            streamEnded = true;
            Logger.info("Loaded " + frameCount + " frames from cache (" + (frameCount * 20 / 1000.0) + "s)");
            
        } catch (Exception e) {
            if (playing) {
                Logger.err("Error loading from cache: " + e.getMessage());
                player.onTrackFail(playingTrack, Severity.FAULT, "Cache load error: " + e.getMessage());
            }
        } finally {
            streamEnded = true;
            loadingFromCache = false;
            Logger.debug("Cache-PCM-Decode thread ended");
        }
    }
    
    private void startCachePopulation(String trackUri) {
        try {
            cacheOutputStream = cacheManager.startSavingTrack(trackUri);
            
            cacheThread = new Thread(() -> {
                try {
                    Logger.debug("Started caching track: " + trackUri);
                    // Cache thread will write PCM data as it's decoded
                    // The decodePcm method will handle the actual writing
                } catch (Exception e) {
                    Logger.err("Error in cache thread: " + e.getMessage());
                }
            }, "Cache-Writer");
            cacheThread.start();
            
        } catch (IOException e) {
            Logger.warn("Failed to start cache population: " + e.getMessage());
            cacheOutputStream = null;
        }
    }
    
    private void finalizeCachePopulation(boolean success) {
        if (cacheOutputStream != null) {
            try {
                cacheOutputStream.close();
            } catch (IOException e) {
                Logger.err("Error closing cache stream: " + e.getMessage());
            }
            cacheOutputStream = null;
        }
        
        if (currentTrackUri != null) {
            cacheManager.finalizeSave(currentTrackUri, success);
            currentTrackUri = null;
        }
        
        if (cacheThread != null && cacheThread.isAlive()) {
            cacheThread.interrupt();
        }
    }

    private void startPipeline(String url) throws IOException {
        // Stop any existing pipeline
        stopPipeline();
        
        // Clear the buffer
        opusBuffer.clear();
        pcmBuffer.clear();
        paused = false;
        playing = true;
        buffering = true;
        streamEnded = false;
        
        // Reset encoder state
        encoder.resetState();
        
        File ytDlpExecutable = getYtDlpExecutable();
        File ffmpegExecutable = getFfmpegExecutable();
        
        Logger.info("Using yt-dlp from: " + ytDlpExecutable.getAbsolutePath());
        Logger.info("Using ffmpeg from: " + ffmpegExecutable.getAbsolutePath());
        
        // Verify yt-dlp is accessible and executable
        if (!ytDlpExecutable.exists()) {
            throw new IOException("yt-dlp not found at: " + ytDlpExecutable.getAbsolutePath());
        }
        if (!ytDlpExecutable.canExecute()) {
            throw new IOException("yt-dlp is not executable: " + ytDlpExecutable.getAbsolutePath() + 
                "\nPlease run: chmod +x \"" + ytDlpExecutable.getAbsolutePath() + "\"");
        }
        
        // 1️⃣ yt-dlp process (with cipher API configuration)
        ProcessBuilder ytDlpPb = new ProcessBuilder(
                ytDlpExecutable.getAbsolutePath(),
                "--extractor-args", "youtubejsc-api:api_url=%s;api_token=%s".formatted(ConfigFile.cipherServerURl, ConfigFile.cipherServerApiKey),
                "-f", "bestaudio",
                "-v", //DEBUG
                "-o", "-",  // pipe to stdout
                "--no-playlist",
                "--cookies", "cookies.txt",
                url
        );
        
        // Set working directory to the yt-dlp location
        ytDlpPb.directory(ytDlpExecutable.getParentFile());
        ytDlpPb.redirectError(ProcessBuilder.Redirect.INHERIT);
        
        Logger.info("Starting yt-dlp with command: " + String.join(" ", ytDlpPb.command()));
        ytDlpProcess = ytDlpPb.start();

        // 2️⃣ FFmpeg process
        ProcessBuilder ffmpegPb = new ProcessBuilder(
                ffmpegExecutable.getAbsolutePath(),
                "-loglevel", "error",
                "-i", "pipe:0",
                "-f", "s16le",
                "-ar", "48000",
                "-ac", "2",
                "pipe:1"
        );
        ffmpegPb.directory(ffmpegExecutable.getParentFile());
        ffmpegPb.redirectError(ProcessBuilder.Redirect.INHERIT);
        ffmpegProcess = ffmpegPb.start();

        // 3️⃣ Pipe yt-dlp -> FFmpeg
        pipeThread = new Thread(() -> {
            try (InputStream ytOut = ytDlpProcess.getInputStream();
                 OutputStream ffIn = ffmpegProcess.getOutputStream()) {
                ytOut.transferTo(ffIn);
            } catch (IOException e) {
                if (playing) {
                    Logger.err("Error in YT-DLP pipe: " + e.getMessage());
                }
            }
        }, "YT-DLP-Pipe");
        pipeThread.start();

        // 4️⃣ Decode PCM frames from FFmpeg
        decodeThread = new Thread(this::decodePcm, "PCM-Decoder");
        decodeThread.start();
        
        // 5️⃣ Encode PCM to Opus in background
        encoderThread = new Thread(this::encodeToOpus, "Opus-Encoder");
        encoderThread.start();

        // Set priorities: Encoder > Decoder > Pipe (encoder is most time-critical)
        // Use more balanced priorities to avoid starving other threads
        pipeThread.setPriority(Thread.NORM_PRIORITY);
        decodeThread.setPriority(Thread.NORM_PRIORITY + 2);
        encoderThread.setPriority(Thread.NORM_PRIORITY + 3); // High but not MAX to avoid system issues

    }
    
    private void stopPipeline() {
        playing = false;
        
        if (ytDlpProcess != null && ytDlpProcess.isAlive()) {
            ytDlpProcess.destroy();
            try {
                ytDlpProcess.waitFor(2, TimeUnit.SECONDS);
                if (ytDlpProcess.isAlive()) {
                    ytDlpProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
            ffmpegProcess.destroy();
            try {
                ffmpegProcess.waitFor(2, TimeUnit.SECONDS);
                if (ffmpegProcess.isAlive()) {
                    ffmpegProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Interrupt and wait for threads to finish
        if (pipeThread != null && pipeThread.isAlive()) {
            pipeThread.interrupt();
            try {
                pipeThread.join(1000); // Wait up to 1 second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (decodeThread != null && decodeThread.isAlive()) {
            decodeThread.interrupt();
            try {
                decodeThread.join(1000); // Wait up to 1 second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (encoderThread != null && encoderThread.isAlive()) {
            encoderThread.interrupt();
            try {
                encoderThread.join(1000); // Wait up to 1 second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Nullify thread references to ensure clean state
        pipeThread = null;
        decodeThread = null;
        encoderThread = null;
    }

    private void decodePcm() {
        try (InputStream pcmIn = ffmpegProcess.getInputStream()) {
            byte[] byteBuffer = new byte[frameSize * 2 * 2]; // 16-bit * 2 channels
            int bytesRead;
            int frameCount = 0;

            while (playing && (bytesRead = pcmIn.readNBytes(byteBuffer, 0, byteBuffer.length)) > 0) {
                // Wait if paused
                while (paused && playing) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        return;
                    }
                }

                if (!playing) break;

                // If we got less than a full frame, pad with zeros
                if (bytesRead < byteBuffer.length) {
                    for (int i = bytesRead; i < byteBuffer.length; i++) {
                        byteBuffer[i] = 0;
                    }
                }
                
                // Write to cache if caching is enabled
                if (cacheOutputStream != null) {
                    try {
                        cacheOutputStream.write(byteBuffer, 0, byteBuffer.length);
                    } catch (IOException e) {
                        Logger.warn("Cache write error: " + e.getMessage());
                        // Close and finalize cache on error
                        try {
                            cacheOutputStream.close();
                        } catch (IOException ignored) {}
                        cacheOutputStream = null;
                        finalizeCachePopulation(false);
                    }
                }

                // Convert bytes to shorts (without applying volume)
                short[] samples = new short[frameSize * 2]; // stereo
                for (int i = 0; i < frameSize * 2; i++) {
                    int lo = byteBuffer[i * 2] & 0xFF;
                    int hi = byteBuffer[i * 2 + 1];
                    samples[i] = (short) ((hi << 8) | lo);
                }

                // Store raw PCM samples (with timeout to prevent blocking)
                if (!pcmBuffer.offer(samples, BUFFER_PUT_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    Logger.warn("PCM buffer full, dropping frame to prevent blocking (live stream)");
                    // Continue anyway, don't lose track of frame count
                }
                frameCount++;

                // Initial buffering
                if (buffering && pcmBuffer.size() >= MIN_BUFFER_SIZE) {
                    buffering = false;
                    Logger.info("Initial buffering complete (" + pcmBuffer.size() + " frames, " + (pcmBuffer.size() * 20 / 1000.0) + "s)");
                    player.onTrackStart(playingTrack);
                }

                // Warn if buffer is getting very full (could indicate consumer is too slow)
                if (frameCount % 500 == 0) { // Every 10 seconds of encoded audio
                    int bufSize = pcmBuffer.size();
                    if (bufSize > 13500) { // >90% full
                        Logger.warn("Buffer critically full: " + bufSize + " frames (" + (bufSize * 20 / 1000.0) + "s) - may cause blocking");
                        player.onTrackFail(playingTrack, Severity.SUSPICIOUS, "PCM Buffer Full, this means the track is too big to Play!!!");
                    }
                }
            }

            streamEnded = true;
            Logger.debug("PCM stream exhausted, processed " + frameCount + " frames (" + (frameCount * 20 / 1000.0) + "s of audio)");
            
            // Finalize cache on successful completion
            if (frameCount > 0 && cacheOutputStream != null) {
                finalizeCachePopulation(true);
            }
            
            if(frameCount == 0){
                finalizeCachePopulation(false);
                throw new RuntimeException("Couldnt start Playing track, See DTS Server logs for details...");
            }
        } catch (Exception e) {
            if (playing) {
                Logger.err("Error in PCM decode: " + e.getMessage());
            }
            finalizeCachePopulation(false);
            player.onTrackFail(playingTrack, Severity.FAULT, e.getMessage());
        } finally {
            streamEnded = true;
            Logger.debug("PCM-Decode thread ended");
        }
    }

    private void encodeToOpus() {
        try {
            byte[] opusBufferArray = new byte[4000]; // max opus frame size
            int encodedCount = 0;

            while (playing) {
                // Wait if paused (with reduced sleep for faster response)
                while (paused && playing) {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        return;
                    }
                }

                if (!playing) break;

                // Adaptive waiting based on buffer status
                int opusBufferSize = opusBuffer.size();
                
                // If buffer is full, sleep briefly to reduce CPU usage
                if (opusBufferSize >= OPUS_BUFFER_TARGET) {
                    try {
                        Thread.sleep(10); // Reduced sleep when buffer is full (was 20ms)
                    } catch (InterruptedException e) {
                        return;
                    }
                    continue;
                }
                
                // If buffer is low, prioritize encoding with shorter timeout
                int pollTimeout = opusBufferSize < OPUS_BUFFER_LOW ? 5 : 25; // Reduced timeouts for faster response

                // Get PCM samples from buffer (adaptive timeout)
                short[] samples;
                try {
                    samples = pcmBuffer.poll(pollTimeout, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    return;
                }

                if (samples == null) {
                    // No PCM available yet, check if stream ended
                    if (streamEnded && pcmBuffer.isEmpty()) {
                        break; // Done encoding
                    }
                    // Don't yield, just continue with short sleep to prevent busy-waiting
                    continue;
                }

                // Apply volume to samples
                for (int i = 0; i < samples.length; i++) {
                    samples[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, samples[i] * volume));
                }

                // Encode to Opus
                int opusLen = encoder.encode(samples, 0, frameSize, opusBufferArray, 0, opusBufferArray.length);
                byte[] frame = new byte[opusLen];
                System.arraycopy(opusBufferArray, 0, frame, 0, opusLen);

                // Add to Opus buffer (with timeout to prevent blocking)
                if (!opusBuffer.offer(frame, BUFFER_PUT_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    Logger.warn("Opus buffer full, dropping frame to prevent blocking");
                }
                encodedCount++;
            }

            Logger.debug("Encoded " + encodedCount + " Opus frames (" + (encodedCount * 20 / 1000.0) + "s of audio)");
        } catch (Exception e) {
            if (playing) {
                Logger.err("Error in Opus encoder: " + e.getMessage());
            }
            player.onTrackFail(playingTrack, Severity.FAULT, e.getMessage());
        } finally {
            Logger.debug("Opus-Encoder thread ended");
        }
    }

    public boolean canProvide() {
        if (paused || !playing) {
            return false;
        }

        // If we're buffering, don't provide yet
        if (buffering) {
            return false;
        }
        
        // Debug: Log if we have data but can't provide for some reason
        if (opusBuffer.isEmpty() && !pcmBuffer.isEmpty() && !streamEnded) {
            Logger.debug("canProvide: Opus buffer empty but PCM buffer has " + pcmBuffer.size() + " frames. Encoder may be stalled.");
        }
        
        int bufferSize = pcmBuffer.size();
        
        // If buffer is critically low and stream hasn't ended, enter rebuffering
        if (bufferSize < REBUFFER_THRESHOLD && !streamEnded && !buffering) {
            buffering = true;

            Logger.warn("Buffer underrun detected (" + bufferSize + " PCM frames), rebuffering...");
            player.onTrackFail(playingTrack, Severity.SUSPICIOUS, "Buffer underflow, rebuffering.. Could lead to unexpected pause. This shouldn't happen normally!");
            return false;
        }
        
        // Resume from rebuffering when we have enough frames again
        if (buffering && bufferSize >= MIN_BUFFER_SIZE) {
            buffering = false;
            Logger.info("Rebuffering complete (" + bufferSize + " PCM frames)");
        }
        
        // Check if playback has naturally ended (both buffers empty, stream ended, not paused, still playing)
        if (opusBuffer.isEmpty() && pcmBuffer.isEmpty() && streamEnded && !paused && playing) {
            playing = false; // Mark as not playing to avoid sending multiple end packets
            Logger.info("Track playback completed naturally");
            player.onTrackEnd(playingTrack, EndReason.FINISHED);
            return false;
        }
        return !opusBuffer.isEmpty();
    }

    public byte[] provide20MsOpus() {
        if (playingTrack != null) {
            playingTrack.setPosition(playingTrack.getPosition() + 20);
        }
        
        // Get pre-encoded Opus frame
        byte[] frame = opusBuffer.poll();
        
        // Log buffer state periodically for debugging
        if (playingTrack != null && playingTrack.getPosition() % 5000 == 0) { // every 5 seconds
            Logger.debug("PCM Buffer: " + pcmBuffer.size() + " frames (" + (pcmBuffer.size() * 20 / 1000.0) + "s), " +
                        "Opus Buffer: " + opusBuffer.size() + " frames (" + (opusBuffer.size() * 20 / 1000.0) + "s), " +
                        "buffering: " + buffering + ", volume: " + volume);
        }
        
        return frame;
    }


    public void stop() {
        if (playingTrack != null && playing) {
            player.onTrackEnd(playingTrack, EndReason.STOPPED);
        }
        
        Logger.debug("Stopping HQ Opus Streamer");
        finalizeCachePopulation(false); // Stop was called, don't save incomplete cache
        stopPipeline();
        pcmBuffer.clear();
        opusBuffer.clear();
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(5.0f, volume)); // Clamp between 0 and 5x
        Logger.debug("Volume changed to: " + this.volume);
    }

    public float getVolume() {
        return this.volume;
    }

    public boolean isPaused() {
        return paused;
    }

    public void pause() {
        player.onPause();
        
        Logger.debug("Pausing HQ Opus Streamer");
        paused = true;
    }

    public void resume() {
        player.onResume();
        
        Logger.debug("Resuming HQ Opus Streamer");
        paused = false;
    }

    public void playTrack(AudioTrack trackObject) {
        String url = trackObject.getInfo().uri;
        
        // If currently playing, notify that old track was replaced
        if (playing && playingTrack != null) {
            Logger.info("Replacing currently playing track: " + playingTrack.getInfo().title);
            player.onTrackEnd(playingTrack, EndReason.REPLACED);
        }
        
        playingTrack = trackObject;
        currentTrackUri = url;
        
        // Ensure volume is synced with current default volume before playing
        volume = player.getDefaultVolume();
        
        try {
            // Check if track is cached
            if (cacheManager.hasTrack(url)) {
                Logger.info("Loading cached track: " + trackObject.getInfo().title + " (volume: " + volume + ")");
                loadFromCache(url);
            } else {
                Logger.info("Playing track with HQ Opus Streamer: " + trackObject.getInfo().title + " (volume: " + volume + ")");
                // Start caching for this track
                startCachePopulation(url);
                startPipeline(url);
            }
        } catch (IOException e) {
            Logger.err("Failed to start pipeline for track: " + e.getMessage());
            player.onTrackFail(playingTrack, Severity.FAULT, "Failed to start playback: " + e.getMessage());
            throw new RuntimeException("Failed to play track", e);
        }
    }

    public AudioTrack getPlayingTrack() {
        return playingTrack;
    }

}
