package io.lolyay.discordmsend.server.music;

/**
 * Utility class for Opus audio handling
 */
public class OpusUtils {
    
    /**
     * Standard sample rate for Discord voice (48kHz)
     */
    public static final int SAMPLE_RATE = 48000;
    
    /**
     * Number of channels (stereo)
     */
    public static final int CHANNELS = 2;
    //67
    /**
     * Frame duration in milliseconds
     */
    public static final int FRAME_DURATION_MS = 20;
    
    /**
     * Frame size in samples (20ms at 48kHz)
     */
    public static final int FRAME_SIZE = 960;
    
    /**
     * Recommended bitrate range
     */
    public static final int MIN_BITRATE = 64000;
    public static final int MAX_BITRATE = 128000;
    public static final int DEFAULT_BITRATE = 64000;
    
    /**
     * Opus silence frame
     */
    public static final byte[] SILENCE_FRAME = new byte[]{(byte) 0xF8, (byte) 0xFF, (byte) 0xFE};
    
    /**
     * Calculates the timestamp increment for a given number of samples
     * @param samples Number of samples
     * @return Timestamp increment
     */
    public static int calculateTimestampIncrement(int samples) {
        return samples;
    }
    
    /**
     * Calculates the duration in milliseconds for a given number of samples
     * @param samples Number of samples
     * @return Duration in milliseconds
     */
    public static long calculateDurationMs(int samples) {
        return (samples * 1000L) / SAMPLE_RATE;
    }
    
    /**
     * Calculates the number of samples for a given duration
     * @param durationMs Duration in milliseconds
     * @return Number of samples
     */
    public static int calculateSamples(long durationMs) {
        return (int) ((durationMs * SAMPLE_RATE) / 1000L);
    }
    
    /**
     * Creates a silence frame
     * @return Opus silence frame bytes
     */
    public static byte[] createSilenceFrame() {
        return SILENCE_FRAME.clone();
    }
    
    /**
     * Validates Opus encoder settings
     * @param sampleRate Sample rate in Hz
     * @param channels Number of channels
     * @param frameSize Frame size in samples
     * @return true if settings are valid for Discord
     */
    public static boolean validateSettings(int sampleRate, int channels, int frameSize) {
        return sampleRate == SAMPLE_RATE && 
               channels == CHANNELS && 
               frameSize == FRAME_SIZE;
    }
    
    /**
     * Gets the expected packet interval in milliseconds
     * @return Packet interval (20ms)
     */
    public static long getPacketInterval() {
        return FRAME_DURATION_MS;
    }
}
