package io.lolyay.discordmsend.client;

import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.AudioS2CPacket;

/**
 * Simple interface for handling audio packets.
 * Implement this interface to receive and process Opus-encoded audio data from the server.
 * 
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * public class MyAudioHandler implements AudioHandler {
 *     @Override
 *     public void onAudioPacket(long guildId, byte[] opusData) {
 *         // Process the opus audio data
 *         // Each packet contains 20ms of Opus-encoded audio
 *         System.out.println("Received " + opusData.length + " bytes for guild " + guildId);
 *         
 *         // Example: Decode with an Opus decoder
 *         // short[] pcm = opusDecoder.decode(opusData);
 *         // playAudio(pcm);
 *     }
 * }
 * 
 * // Register with client event handler
 * client.setEventHandler(new ClientEventHandler() {
 *     private AudioHandler audioHandler = new MyAudioHandler();
 *     
 *     @Override
 *     public void onAudio(AudioS2CPacket packet) {
 *         audioHandler.onAudioPacket(packet.guildId(), packet.opus());
 *     }
 *     // ... implement other methods
 * });
 * }</pre>
 */
public interface AudioHandler {
    /**
     * Called when an audio packet is received from the server.
     * This is only sent for special guild IDs (0-200) that use direct packet transmission.
     * 
     * @param guildId The guild/player ID this audio is for
     * @param opusData Opus-encoded audio data (20ms frame, typically 3-300 bytes depending on complexity)
     */
    void onAudioPacket(long guildId, byte[] opusData);
}
