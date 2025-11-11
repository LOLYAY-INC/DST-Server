package io.lolyay.discordmsend.server.cache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Manages caching of pre-processed audio tracks to avoid repeated yt-dlp/ffmpeg invocations.
 * Tracks are stored by URI hash for fast lookup and retrieval.
 */
public interface AudioCacheManager {
    
    /**
     * Checks if a track with the given URI is cached.
     * 
     * @param trackUri The track URI to check
     * @return true if the track is cached and ready to be loaded
     */
    boolean hasTrack(String trackUri);
    
    /**
     * Gets an InputStream to read cached PCM data for a track.
     * 
     * @param trackUri The track URI to load
     * @return InputStream containing PCM data (s16le, 48kHz, stereo)
     * @throws IOException if track doesn't exist or can't be read
     */
    InputStream loadTrack(String trackUri) throws IOException;
    
    /**
     * Creates an OutputStream to save PCM data for a track.
     * Data should be written as it's being played, and finalized on track end.
     * 
     * @param trackUri The track URI to save
     * @return OutputStream to write PCM data to
     * @throws IOException if cache can't be written
     */
    OutputStream startSavingTrack(String trackUri) throws IOException;
    
    /**
     * Finalizes saving a track after all data has been written.
     * This should be called after the OutputStream from startSavingTrack() is closed.
     * 
     * @param trackUri The track URI that was saved
     * @param success true if track was fully saved, false if it should be discarded
     */
    void finalizeSave(String trackUri, boolean success);
    
    /**
     * Deletes a track from the cache.
     * 
     * @param trackUri The track URI to delete
     * @return true if track was deleted, false if it didn't exist
     */
    boolean deleteTrack(String trackUri);
    
    /**
     * Gets the total size of the cache in bytes.
     * 
     * @return cache size in bytes
     */
    long getCacheSize();
    
    /**
     * Clears the entire cache.
     */
    void clearCache();
    
    /**
     * Computes a hash for a track URI for use as filename/identifier.
     * 
     * @param trackUri The track URI
     * @return Hash string suitable for use as a filename
     */
    default String computeHash(String trackUri) {
        // Simple hash - can be overridden for better hashing
        return Integer.toHexString(trackUri.hashCode());
    }
}
