package io.lolyay.discordmsend.client;

import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.AudioS2CPacket;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.TrackTimingUpdateS2CPacket;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.ev.*;

/**
 * A simple implementation of ClientEventHandler that delegates audio handling to an AudioHandler.
 * Extend this class and override the methods you need, or implement ClientEventHandler directly.
 * 
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Option 1: Use with AudioHandler
 * AudioHandler myAudioHandler = new MyAudioHandler();
 * client.setEventHandler(new SimpleClientEventHandler(myAudioHandler));
 * 
 * // Option 2: Extend and customize
 * client.setEventHandler(new SimpleClientEventHandler(myAudioHandler) {
 *     @Override
 *     public void onPlayerTrackStart(PlayerTrackStartS2CPacket packet) {
 *         System.out.println("Track started: " + packet.guildId());
 *         super.onPlayerTrackStart(packet);
 *     }
 * });
 * }</pre>
 */
public class SimpleClientEventHandler implements ClientEventHandler {
    private final AudioHandler audioHandler;
    
    /**
     * Create a simple event handler with an audio handler
     * @param audioHandler Handler for audio packets, or null if not handling audio
     */
    public SimpleClientEventHandler(AudioHandler audioHandler) {
        this.audioHandler = audioHandler;
    }
    
    /**
     * Create a simple event handler without audio handling
     */
    public SimpleClientEventHandler() {
        this(null);
    }
    
    @Override
    public void onPlayerPause(PlayerPauseS2CPacket packet) {
        // Override to handle pause events
    }
    
    @Override
    public void onPlayerResume(PlayerResumeS2CPacket packet) {
        // Override to handle resume events
    }
    
    @Override
    public void onPlayerTrackStart(PlayerTrackStartS2CPacket packet) {
        // Override to handle track start events
    }
    
    @Override
    public void onPlayerTrackEnd(PlayerTrackEndS2CPacket packet) {
        // Override to handle track end events
    }
    
    @Override
    public void onPlayerTrackError(PlayerTrackFailS2CPacket packet) {
        // Override to handle track error events
    }
    
    @Override
    public void onPlayerTrackStuck(PlayerTrackStuckS2CPacket packet) {
        // Override to handle track stuck events
    }
    
    @Override
    public void onDisconnect(String reason) {
        // Override to handle disconnect events
    }
    
    @Override
    public void onTrackTimingUpdate(TrackTimingUpdateS2CPacket packet) {
        // Override to handle timing update events
    }
    
    @Override
    public void onAudio(AudioS2CPacket packet) {
        if (audioHandler != null) {
            audioHandler.onAudioPacket(packet.guildId(), packet.opus());
        }
    }
}
