package io.lolyay.discordmsend.server.cache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class FileSystemAudioCacheManager implements AudioCacheManager {
    
    private final Path cacheDirectory;
    private final Path indexFile;
    private final Map<String, String> trackIndex;
    private final Gson gson;
    private final Map<String, Path> pendingSaves;
    
    public FileSystemAudioCacheManager(String cacheDirectoryPath) throws IOException {
        this.cacheDirectory = Paths.get(cacheDirectoryPath);
        this.indexFile = cacheDirectory.resolve("index.json");
        this.trackIndex = new ConcurrentHashMap<>();
        this.pendingSaves = new ConcurrentHashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        if (!Files.exists(cacheDirectory)) {
            Files.createDirectories(cacheDirectory);
            log.info("Created track cache directory: {}", cacheDirectory.toAbsolutePath());
        }
        
        loadIndex();
        log.info("Track cache initialized with {} cached tracks", trackIndex.size());
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
            trackIndex.remove(hash);
            saveIndex();
            throw new IOException("Track file missing: " + filename);
        }
        
        log.info("Loading cached track: " + filename + " (" + Files.size(trackFile) / 1024 / 1024 + " MB)");
        return new BufferedInputStream(Files.newInputStream(trackFile));
    }
    
    @Override
    public OutputStream startSavingTrack(String trackUri) throws IOException {
        String hash = computeHash(trackUri);
        String tempFilename = hash + ".pcm.tmp";
        Path tempFile = cacheDirectory.resolve(tempFilename);
        
        pendingSaves.put(trackUri, tempFile);
        
        log.debug("Starting to cache track: " + hash);
        return new BufferedOutputStream(Files.newOutputStream(tempFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
    }
    
    @Override
    public void finalizeSave(String trackUri, boolean success) {
        Path tempFile = pendingSaves.remove(trackUri);
        
        if (tempFile == null) {
            log.warn("No pending save found for track: " + trackUri);
            return;
        }
        
        if (!success || !Files.exists(tempFile)) {
            try {
                if (Files.exists(tempFile)) {
                    Files.delete(tempFile);
                }
                log.debug("Deleted incomplete cache file: " + tempFile.getFileName());
            } catch (IOException e) {
                log.error("Failed to delete incomplete cache file: " + e.getMessage());
            }
            return;
        }
        
        try {
            String hash = computeHash(trackUri);
            String finalFilename = hash + ".pcm";
            Path finalFile = cacheDirectory.resolve(finalFilename);
            
            if (Files.exists(finalFile)) {
                Files.delete(finalFile);
            }
            
            Files.move(tempFile, finalFile, StandardCopyOption.REPLACE_EXISTING);
            
            trackIndex.put(hash, finalFilename);
            saveIndex();
            
            long sizeKB = Files.size(finalFile) / 1024;
            log.info("Successfully cached track: " + finalFilename + " (" + sizeKB + " KB)");
            
        } catch (IOException e) {
            log.error("Failed to finalize track cache: " + e.getMessage());
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
                log.info("Deleted cached track: " + filename);
            }
            return deleted;
            
        } catch (IOException e) {
            log.error("Failed to delete track: " + e.getMessage());
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
            log.error("Failed to calculate cache size: " + e.getMessage());
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
                            log.error("Failed to delete: " + p.getFileName());
                        }
                    });
            
            trackIndex.clear();
            saveIndex();
            log.info("Cache cleared");
            
        } catch (IOException e) {
            log.error("Failed to clear cache: " + e.getMessage());
        }
    }
    
    @Override
    public String computeHash(String trackUri) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(trackUri.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.substring(0, 16);
            
        } catch (NoSuchAlgorithmException e) {
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
            log.error("Failed to load cache index: " + e.getMessage());
        }
    }
    
    private void saveIndex() {
        try (Writer writer = Files.newBufferedWriter(indexFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            gson.toJson(trackIndex, writer);
        } catch (IOException e) {
            log.error("Failed to save cache index: " + e.getMessage());
        }
    }
}
