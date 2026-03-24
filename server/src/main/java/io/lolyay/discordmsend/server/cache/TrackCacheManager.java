package io.lolyay.discordmsend.server.cache;

import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.CacheExpireS2C;
import io.lolyay.discordmsend.server.DstServer;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TrackCacheManager {
    
    private final Map<Integer, Long> trackLastAccessTime = new ConcurrentHashMap<>();
    private final long ttlMillis;
    private final DstServer dstServer;
    
    public TrackCacheManager(DstServer dstServer, int ttlDays) {
        this.dstServer = dstServer;
        this.ttlMillis = TimeUnit.DAYS.toMillis(ttlDays);
    }
    
    public void markAccessed(int trackId) {
        trackLastAccessTime.put(trackId, System.currentTimeMillis());
    }
    
    public void expireOldTracks() {
        long now = System.currentTimeMillis();
        IntArrayList expiredIds = new IntArrayList();
        
        for (Map.Entry<Integer, Long> entry : trackLastAccessTime.entrySet()) {
            int trackId = entry.getKey();
            long lastAccess = entry.getValue();
            
            if (now - lastAccess > ttlMillis) {
                expiredIds.add(trackId);
            }
        }
        
        if (expiredIds.isEmpty()) {
            return;
        }
        
        for (int trackId : expiredIds) {
            trackLastAccessTime.remove(trackId);
        }
        
        dstServer.removeTracksFromCache(expiredIds);
        
        CacheExpireS2C packet = new CacheExpireS2C(expiredIds);
        dstServer.broadcastToAllClients(packet);
        
        log.info("Expired " + expiredIds.size() + " tracks from cache (unused for " + 
                    TimeUnit.MILLISECONDS.toDays(ttlMillis) + " days)");

    }
    public int getTrackedCount() {
        return trackLastAccessTime.size();
    }
    
    public void remove(int trackId) {
        trackLastAccessTime.remove(trackId);
    }
    
    public void clear() {
        trackLastAccessTime.clear();
    }
}
