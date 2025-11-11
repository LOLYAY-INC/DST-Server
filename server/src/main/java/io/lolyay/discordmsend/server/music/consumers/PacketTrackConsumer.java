package io.lolyay.discordmsend.server.music.consumers;

import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.AudioS2CPacket;
import io.lolyay.discordmsend.network.types.ClientFeatures;
import io.lolyay.discordmsend.server.music.players.TrackPlayerInstance;
import io.lolyay.discordmsend.server.music.providers.HighQualityOpusStreamer;
import io.lolyay.discordmsend.util.logging.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Sends AudioS2CPackets directly to non-Discord bot clients.
 * Starts immediately upon construction and only exposes stop().
 */
public class PacketTrackConsumer implements ITrackConsumer {
    private static final int FRAME_INTERVAL_MS = 20;
    
    private final TrackPlayerInstance parent;
    private final long guildId;
    private final BlockingQueue<byte[]> audioQueue;
    private final Thread senderThread;
    private final Thread generatorThread;
    private final HighQualityOpusStreamer audioProvider;
    private final boolean udpMode;
    
    private volatile boolean running = false;
    private volatile boolean generating = false;
    
    public PacketTrackConsumer(TrackPlayerInstance parent, HighQualityOpusStreamer streamer) {
        this.parent = parent;
        this.guildId = parent.getGuildId();
        this.audioProvider = streamer;
        
        // Check if client has UDP_ME_PLZ feature
        this.udpMode = parent.getParent().getOwner().getUserData() != null &&
                       parent.getParent().getOwner().getUserData().features().contains(
                           ClientFeatures.Feature.UDP_ME_PLZ);
        
        // Larger queue for UDP mode to allow faster buffering
        this.audioQueue = new LinkedBlockingQueue<>(udpMode ? 50000 : 1000);
        
        Logger.info("Created PacketTrackConsumer for guild " + guildId + " (UDP mode: " + udpMode + ")");
        
        // Create threads (will be started in start())
        this.senderThread = new Thread(this::senderLoop, "SpecialPlayer-Sender-" + guildId);
        this.senderThread.setDaemon(true);
        
        this.generatorThread = new Thread(this::generatorLoop, "SpecialPlayer-Generator-" + guildId);
        this.generatorThread.setDaemon(true);
    }
    
    private void senderLoop() {
        Logger.info("Sender thread started for guild " + guildId);
        
        while (running) {
            try {
                // Get next audio frame
                byte[] opusData = audioQueue.poll(udpMode ? 10 : 100, TimeUnit.MILLISECONDS);
                if (opusData == null) {
                    continue;
                }
                
                // Send AudioS2CPacket
                AudioS2CPacket packet = new AudioS2CPacket(guildId, opusData);
                parent.getParent().getOwner().sendPacket(packet);
                

            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                Logger.err("Error in sender loop: " + e.getMessage());
            }
        }
        
        Logger.info("Sender thread stopped for guild " + guildId);
    }
    
    private void generatorLoop() {
        Logger.info("Generator thread started for guild " + guildId + " (UDP mode: " + udpMode + ")");
        
        try {
            if (udpMode) {
                // UDP MODE: Pull and queue as fast as possible
                while (generating && running) {
                    if (audioProvider.canProvide()) {
                        byte[] opusFrame = audioProvider.provide20MsOpus();
                        if (opusFrame != null) {
                            if (!audioQueue.offer(opusFrame, 1, TimeUnit.SECONDS)) {
                                Logger.warn("Audio queue full, dropping frame");
                            }
                        }
                    } else {
                        Thread.yield();
                    }
                }
            } else {
                // NORMAL MODE: Every 20ms pull data or send silence
                long startTime = System.nanoTime();
                int frameCount = 0;
                
                while (generating && running) {
                    long targetTime = startTime + (frameCount * FRAME_INTERVAL_MS * 1_000_000L);
                    long currentTime = System.nanoTime();
                    long sleepTime = (targetTime - currentTime) / 1_000_000L;
                    
                    if (sleepTime > 0) {
                        Thread.sleep(sleepTime);
                    }
                    
                    byte[] opusFrame;
                    if (audioProvider.canProvide()) {
                        opusFrame = audioProvider.provide20MsOpus();
                        if (opusFrame == null) {
                            opusFrame = new byte[]{(byte) 0xF8, (byte) 0xFF, (byte) 0xFE}; // Silent frame
                        }
                    } else {
                        opusFrame = new byte[]{(byte) 0xF8, (byte) 0xFF, (byte) 0xFE}; // Silent frame
                    }
                    
                    if (!audioQueue.offer(opusFrame, 1, TimeUnit.SECONDS)) {
                        Logger.warn("Audio queue full, dropping frame");
                    }
                    
                    frameCount++;
                }
            }
        } catch (InterruptedException e) {
            Logger.debug("Generator thread interrupted");
        } catch (Exception e) {
            Logger.err("Error in generator loop: " + e.getMessage());
        }
        
        Logger.info("Generator thread stopped for guild " + guildId);
    }
    @Override
    public HighQualityOpusStreamer getStreamer() {
        return audioProvider;
    }
    public void stop() {
        Logger.info("Stopping SpecialThreadPlayer for guild " + guildId);
        audioProvider.stop();
        generating = false;
        running = false;
        audioQueue.clear();
        
        if (generatorThread != null && generatorThread.isAlive()) {
            generatorThread.interrupt();
            try {
                generatorThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        if (senderThread != null && senderThread.isAlive()) {
            senderThread.interrupt();
            try {
                senderThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void start() {
        Logger.info("Starting PacketTrackConsumer threads for guild " + guildId);
        running = true;
        generating = true;
        senderThread.start();
        generatorThread.start();
    }
}
