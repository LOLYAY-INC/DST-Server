package io.lolyay.discordmsend.server.upload;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.lolyay.discordmsend.util.logging.Logger;

import java.io.*;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the mapping between track cache IDs and their uploaded URLs.
 * Stores data in uploaddata.json file.
 */
public class UploadDataManager {
    
    private final Path uploadDataFile;
    private final Map<String, String> cacheIdToUrlMap; // cacheId -> URL
    private final Gson gson;
    
    public UploadDataManager(String uploadDataPath) throws IOException {
        this.uploadDataFile = Paths.get(uploadDataPath);
        this.cacheIdToUrlMap = new ConcurrentHashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        // Create parent directory if needed
        Path parent = uploadDataFile.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        
        // Load existing data
        loadData();
        Logger.info("Upload data manager initialized with " + cacheIdToUrlMap.size() + " cached URLs");
    }
    
    /**
     * Checks if a URL exists for the given cache ID.
     */
    public boolean hasUrl(String cacheId) {
        return cacheIdToUrlMap.containsKey(cacheId);
    }
    
    /**
     * Gets the URL for a cache ID, or null if not found.
     */
    public String getUrl(String cacheId) {
        return cacheIdToUrlMap.get(cacheId);
    }
    
    /**
     * Saves a URL for a cache ID and persists to disk.
     */
    public void saveUrl(String cacheId, String url) {
        cacheIdToUrlMap.put(cacheId, url);
        saveData();
        Logger.info("Saved upload URL for cache ID: " + cacheId);
    }
    
    /**
     * Removes a URL mapping.
     */
    public void removeUrl(String cacheId) {
        cacheIdToUrlMap.remove(cacheId);
        saveData();
    }
    
    /**
     * Gets the total number of cached URLs.
     */
    public int size() {
        return cacheIdToUrlMap.size();
    }
    
    /**
     * Clears all URL mappings.
     */
    public void clear() {
        cacheIdToUrlMap.clear();
        saveData();
    }
    
    private void loadData() {
        if (!Files.exists(uploadDataFile)) {
            Logger.debug("Upload data file doesn't exist, starting fresh");
            return;
        }
        
        try (Reader reader = Files.newBufferedReader(uploadDataFile)) {
            Map<String, String> loaded = gson.fromJson(reader, new TypeToken<Map<String, String>>(){}.getType());
            if (loaded != null) {
                cacheIdToUrlMap.putAll(loaded);
            }
        } catch (IOException e) {
            Logger.err("Failed to load upload data: " + e.getMessage());
        }
    }
    
    private void saveData() {
        try (Writer writer = Files.newBufferedWriter(uploadDataFile, 
                StandardOpenOption.CREATE, 
                StandardOpenOption.TRUNCATE_EXISTING)) {
            gson.toJson(cacheIdToUrlMap, writer);
        } catch (IOException e) {
            Logger.err("Failed to save upload data: " + e.getMessage());
        }
    }
}
