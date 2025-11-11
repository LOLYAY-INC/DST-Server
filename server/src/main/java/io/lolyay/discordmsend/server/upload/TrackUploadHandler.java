package io.lolyay.discordmsend.server.upload;

import io.lolyay.discordmsend.util.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles the process of converting PCM files to MP3 and uploading to S3.
 */
public class TrackUploadHandler {
    
    private final UploadDataManager uploadDataManager;
    private final S3UploadManager s3UploadManager;
    private final ExecutorService uploadExecutor;
    private final Map<String, CompletableFuture<String>> activeUploads;
    private final String tempDir;
    
    public TrackUploadHandler(UploadDataManager uploadDataManager, S3UploadManager s3UploadManager) {
        this.uploadDataManager = uploadDataManager;
        this.s3UploadManager = s3UploadManager;
        this.uploadExecutor = Executors.newFixedThreadPool(3); // Max 3 concurrent uploads
        this.activeUploads = new ConcurrentHashMap<>();
        this.tempDir = "./cache/temp_mp3";
        
        // Create temp directory
        try {
            Files.createDirectories(Paths.get(tempDir));
        } catch (IOException e) {
            Logger.err("Failed to create temp directory: " + e.getMessage());
        }
        
        Logger.info("Track Upload Handler initialized");
    }
    
    /**
     * Gets or creates an upload URL for a track.
     * If the URL is already cached, returns it immediately.
     * If upload is in progress, returns the existing future.
     * Otherwise, starts a new upload process.
     * 
     * @param cacheId The cache ID of the track
     * @param pcmFilePath Path to the PCM file
     * @return CompletableFuture that completes with the URL or fails with exception
     */
    public CompletableFuture<String> getOrUploadTrack(String cacheId, String pcmFilePath) {
        // Check if URL already exists
        String existingUrl = uploadDataManager.getUrl(cacheId);
        if (existingUrl != null) {
            Logger.debug("URL already cached for: " + cacheId);
            return CompletableFuture.completedFuture(existingUrl);
        }
        
        // Check if upload is already in progress
        CompletableFuture<String> existing = activeUploads.get(cacheId);
        if (existing != null) {
            Logger.debug("Upload already in progress for: " + cacheId);
            return existing;
        }
        
        // Start new upload
        CompletableFuture<String> uploadFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return performUpload(cacheId, pcmFilePath);
            } catch (Exception e) {
                Logger.err("Upload failed for " + cacheId + ": " + e.getMessage());
                throw new RuntimeException(e);
            } finally {
                activeUploads.remove(cacheId);
            }
        }, uploadExecutor);
        
        activeUploads.put(cacheId, uploadFuture);
        return uploadFuture;
    }
    
    private String performUpload(String cacheId, String pcmFilePath) throws Exception {
        Logger.info("Starting upload process for: " + cacheId);
        
        // Step 1: Convert PCM to MP3
        File mp3File = convertPcmToMp3(pcmFilePath, cacheId);
        
        try {
            // Step 2: Upload to S3
            String url = s3UploadManager.uploadTrack(mp3File, cacheId);
            
            // Step 3: Save URL to mapping
            uploadDataManager.saveUrl(cacheId, url);
            
            return url;
            
        } finally {
            // Clean up temp MP3 file
            if (mp3File.exists()) {
                try {
                    Files.delete(mp3File.toPath());
                    Logger.debug("Deleted temp MP3: " + mp3File.getName());
                } catch (IOException e) {
                    Logger.warn("Failed to delete temp MP3: " + e.getMessage());
                }
            }
        }
    }
    
    private File convertPcmToMp3(String pcmFilePath, String cacheId) throws Exception {
        Path pcmPath = Paths.get(pcmFilePath);
        if (!Files.exists(pcmPath)) {
            throw new IOException("PCM file not found: " + pcmFilePath);
        }
        
        File mp3File = new File(tempDir, cacheId + ".mp3");
        
        Logger.info("Converting PCM to MP3: " + pcmPath.getFileName());
        
        // Build ffmpeg command
        // Input: s16le (signed 16-bit little-endian PCM), 48kHz, stereo
        ProcessBuilder pb = new ProcessBuilder(
                getFfmpegExecutable().getAbsolutePath(),
                "-f", "s16le",           // Input format
                "-ar", "48000",          // Sample rate
                "-ac", "2",              // Channels (stereo)
                "-i", pcmFilePath,       // Input file
                "-codec:a", "libmp3lame", // MP3 encoder
                "-b:a", "320k",          // 320 kbps bitrate
                "-y",                    // Overwrite output
                mp3File.getAbsolutePath()
        );
        
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        // Wait for conversion
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new IOException("FFmpeg conversion failed with exit code: " + exitCode);
        }
        
        if (!mp3File.exists()) {
            throw new IOException("MP3 file was not created");
        }
        
        long mp3Size = mp3File.length() / 1024; // KB
        Logger.info("Successfully converted to MP3 (" + mp3Size + " KB)");
        
        return mp3File;
    }
    
    private File getFfmpegExecutable() {
        String os = System.getProperty("os.name").toLowerCase();
        String execName = os.contains("win") ? "ffmpeg.exe" : "ffmpeg";
        
        // Check in PATH first
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (String path : pathEnv.split(File.pathSeparator)) {
                File exec = new File(path, execName);
                if (exec.exists() && exec.canExecute()) {
                    return exec;
                }
            }
        }
        
        // Check in common locations
        String[] commonPaths = {
                "/usr/bin/" + execName,
                "/usr/local/bin/" + execName,
                "./bin/" + execName,
                "./" + execName
        };
        
        for (String path : commonPaths) {
            File exec = new File(path);
            if (exec.exists() && exec.canExecute()) {
                return exec;
            }
        }
        
        // Fallback: assume it's in PATH
        return new File(execName);
    }
    
    /**
     * Shuts down the upload executor.
     */
    public void shutdown() {
        uploadExecutor.shutdown();
        s3UploadManager.close();
    }
}
