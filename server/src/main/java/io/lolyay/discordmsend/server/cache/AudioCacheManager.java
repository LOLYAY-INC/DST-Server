package io.lolyay.discordmsend.server.cache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public interface AudioCacheManager {

    boolean hasTrack(String trackUri);

    InputStream loadTrack(String trackUri) throws IOException;

    OutputStream startSavingTrack(String trackUri) throws IOException;

    void finalizeSave(String trackUri, boolean success);

    boolean deleteTrack(String trackUri);
    

    long getCacheSize();

    void clearCache();
    

    default String computeHash(String trackUri) {
        return Integer.toHexString(trackUri.hashCode());
    }
}
