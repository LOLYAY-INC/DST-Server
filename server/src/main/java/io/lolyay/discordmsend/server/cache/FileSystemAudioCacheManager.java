package io.lolyay.discordmsend.server.cache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.lolyay.discordmsend.util.logging.Logger;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * File system based implementation of AudioCacheManager.
 * Stores cached tracks in a "tracks" directory with a JSON index for hash -> filename mapping.
 */
public class FileSystemAudioCacheManager implements AudioCacheManager {
    
    private final Path cacheDirectory;
    private final Path indexFile;
    private final Map<String, String> trackIndex; // hash -> filename
    private final Gson gson;
    private final Map<String, Path> pendingSaves; // trackUri -> temp file path
    
    public FileSystemAudioCacheManager(String cacheDirectoryPath) throws IOException {
        this.cacheDirectory = Paths.get(cacheDirectoryPath);
        this.indexFile = cacheDirectory.resolve("index.json");
        this.trackIndex = new ConcurrentHashMap<>();
        this.pendingSaves = new ConcurrentHashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        // Create cache directory if it doesn't exist
        if (!Files.exists(cacheDirectory)) {
            Files.createDirectories(cacheDirectory);
            Logger.info("Created track cache directory: " + cacheDirectory.toAbsolutePath());
        }
        
        // Load existing index
        loadIndex();
        Logger.info("Track cache initialized with " + trackIndex.size() + " cached tracks");
    }
    
    @Override
    public boolean hasTrack(String trackUri) {
        String hash = computeHash(trackUri);
        String filename = trackIndex.get(hash);
        if (filename == null) {
            return false;
        }
        
        Path trackFile = cacheDirectory.resolve(filename);
        return Files.exists(trackFile) && Files.isReadable(trackFile);
    }
    
    @Override
    public InputStream loadTrack(String trackUri) throws IOException {
        String hash = computeHash(trackUri);
        String filename = trackIndex.get(hash);
        
        if (filename == null) {
            throw new IOException("Track not found in cache: " + trackUri);
        }
        
        Path trackFile = cacheDirectory.resolve(filename);
        if (!Files.exists(trackFile)) {
            // Clean up stale index entry
            trackIndex.remove(hash);
            saveIndex();
            throw new IOException("Track file missing: " + filename);
        }
        
        Logger.info("Loading cached track: " + filename + " (" + Files.size(trackFile) / 1024 / 1024 + " MB)");
        return new BufferedInputStream(Files.newInputStream(trackFile));
    }
    
    @Override
    public OutputStream startSavingTrack(String trackUri) throws IOException {
        String hash = computeHash(trackUri);
        String tempFilename = hash + ".pcm.tmp";
        Path tempFile = cacheDirectory.resolve(tempFilename);
        
        // Store temp file path for finalization
        pendingSaves.put(trackUri, tempFile);
        
        Logger.debug("Starting to cache track: " + hash);
        return new BufferedOutputStream(Files.newOutputStream(tempFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
    }
    
    @Override
    public void finalizeSave(String trackUri, boolean success) {
        Path tempFile = pendingSaves.remove(trackUri);
        
        if (tempFile == null) {
            Logger.warn("No pending save found for track: " + trackUri);
            return;
        }
        
        if (!success || !Files.exists(tempFile)) {
            // Delete incomplete file
            try {
                if (Files.exists(tempFile)) {
                    Files.delete(tempFile);
                }
                Logger.debug("Deleted incomplete cache file: " + tempFile.getFileName());
            } catch (IOException e) {
                Logger.err("Failed to delete incomplete cache file: " + e.getMessage());
            }
            return;
        }
        
        try {
            // Move temp file to final location
            String hash = computeHash(trackUri);
            String finalFilename = hash + ".pcm";
            Path finalFile = cacheDirectory.resolve(finalFilename);
            
            // If file already exists, delete it first
            if (Files.exists(finalFile)) {
                Files.delete(finalFile);
            }
            
            Files.move(tempFile, finalFile, StandardCopyOption.REPLACE_EXISTING);
            
            // Update index
            trackIndex.put(hash, finalFilename);
            saveIndex();
            
            long sizeKB = Files.size(finalFile) / 1024;
            Logger.info("Successfully cached track: " + finalFilename + " (" + sizeKB + " KB)");
            
        } catch (IOException e) {
            Logger.err("Failed to finalize track cache: " + e.getMessage());
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ex) {
                // Ignore
            }
        }
    }
    
    @Override
    public boolean deleteTrack(String trackUri) {
        String hash = computeHash(trackUri);
        String filename = trackIndex.remove(hash);
        
        if (filename == null) {
            return false;
        }
        
        try {
            Path trackFile = cacheDirectory.resolve(filename);
            boolean deleted = Files.deleteIfExists(trackFile);
            saveIndex();
            
            if (deleted) {
                Logger.info("Deleted cached track: " + filename);
            }
            return deleted;
            
        } catch (IOException e) {
            Logger.err("Failed to delete track: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public long getCacheSize() {
        try {
            return Files.walk(cacheDirectory)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".pcm"))
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            Logger.err("Failed to calculate cache size: " + e.getMessage());
            return 0;
        }
    }
    
    @Override
    public void clearCache() {
        try {
            Files.walk(cacheDirectory)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".pcm") || p.toString().endsWith(".pcm.tmp"))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            Logger.err("Failed to delete: " + p.getFileName());
                        }
                    });
            
            trackIndex.clear();
            saveIndex();
            Logger.info("Cache cleared");
            
        } catch (IOException e) {
            Logger.err("Failed to clear cache: " + e.getMessage());
        }
    }
    
    @Override
    public String computeHash(String trackUri) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(trackUri.getBytes());
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            // Use first 16 characters for reasonable filename length
            return hexString.substring(0, 16);
            
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hash
            return Integer.toHexString(trackUri.hashCode());
        }
    }
    
    private void loadIndex() {
        if (!Files.exists(indexFile)) {
            return;
        }
        
        try (Reader reader = Files.newBufferedReader(indexFile)) {
            Map<String, String> loaded = gson.fromJson(reader, new TypeToken<Map<String, String>>(){}.getType());
            if (loaded != null) {
                trackIndex.putAll(loaded);
            }
        } catch (IOException e) {
            Logger.err("Failed to load cache index: " + e.getMessage());
        }
    }
    
    private void saveIndex() {
        try (Writer writer = Files.newBufferedWriter(indexFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            gson.toJson(trackIndex, writer);
        } catch (IOException e) {
            Logger.err("Failed to save cache index: " + e.getMessage());
        }
    }
}
