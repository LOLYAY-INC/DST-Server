package io.lolyay.discordmsend.server.cache;

import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.CacheExpireS2C;
import io.lolyay.discordmsend.server.Server;
import io.lolyay.discordmsend.util.logging.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manages track cache expiration. Tracks when each track was last accessed
 * and periodically removes tracks that haven't been used in the configured TTL period.
 */
public class TrackCacheManager {
    
    private final Map<Integer, Long> trackLastAccessTime = new ConcurrentHashMap<>();
    private final long ttlMillis;
    private final Server server;
    
    /**
     * Creates a new cache manager.
     * 
     * @param server The server instance
     * @param ttlDays Number of days before a track expires from cache
     */
    public TrackCacheManager(Server server, int ttlDays) {
        this.server = server;
        this.ttlMillis = TimeUnit.DAYS.toMillis(ttlDays);
    }
    
    /**
     * Marks a track as accessed, updating its last access time.
     * 
     * @param trackId The track ID that was accessed
     */
    public void markAccessed(int trackId) {
        trackLastAccessTime.put(trackId, System.currentTimeMillis());
    }
    
    /**
     * Checks for expired tracks and removes them from cache.
     * Notifies all connected clients about expired tracks.
     * 
     * @return Number of tracks expired
     */
    public int expireOldTracks() {
        long now = System.currentTimeMillis();
        List<Integer> expiredIds = new ArrayList<>();
        
        // Find all expired tracks
        for (Map.Entry<Integer, Long> entry : trackLastAccessTime.entrySet()) {
            int trackId = entry.getKey();
            long lastAccess = entry.getValue();
            
            if (now - lastAccess > ttlMillis) {
                expiredIds.add(trackId);
            }
        }
        
        if (expiredIds.isEmpty()) {
            return 0;
        }
        
        // Remove from tracking
        for (int trackId : expiredIds) {
            trackLastAccessTime.remove(trackId);
        }
        
        // Remove from server cache
        server.removeTracksFromCache(expiredIds);
        
        // Notify all clients
        CacheExpireS2C packet = new CacheExpireS2C(expiredIds);
        server.broadcastToAllClients(packet);
        
        Logger.info("Expired " + expiredIds.size() + " tracks from cache (unused for " + 
                    TimeUnit.MILLISECONDS.toDays(ttlMillis) + " days)");
        
        return expiredIds.size();
    }
    
    /**
     * Gets the number of tracked tracks.
     * 
     * @return Number of tracks being tracked
     */
    public int getTrackedCount() {
        return trackLastAccessTime.size();
    }
    
    /**
     * Manually removes a track from tracking.
     * 
     * @param trackId Track ID to remove
     */
    public void remove(int trackId) {
        trackLastAccessTime.remove(trackId);
    }
    
    /**
     * Clears all tracking data.
     */
    public void clear() {
        trackLastAccessTime.clear();
    }
}
