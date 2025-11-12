package io.lolyay.discordmsend.server;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerRegistry;
import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.getyarn.GetyarnAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.nico.NicoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.yamusic.YandexMusicAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import dev.lavalink.youtube.track.YoutubeAudioTrack;
import io.lolyay.discordmsend.network.protocol.Connection;
import io.lolyay.discordmsend.network.protocol.NetworkPhase;
import io.lolyay.discordmsend.network.protocol.encryption.NetworkEncryptionUtils;
import io.lolyay.discordmsend.network.protocol.packet.PacketRegistry;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.KeepAliveS2CPacket;
import io.lolyay.discordmsend.network.types.ModdedInfo;
import io.lolyay.discordmsend.network.types.ServerFeatures;
import io.lolyay.discordmsend.obj.MusicTrack;
import io.lolyay.discordmsend.server.cache.TrackCacheManager;
import io.lolyay.discordmsend.server.network.ConnectedClient;
import io.lolyay.discordmsend.server.network.NetworkServer;
import io.lolyay.discordmsend.server.yt.NoOpAudioTrackHelper;
import io.lolyay.discordmsend.server.yt.YoutubeCipherH;
import io.lolyay.discordmsend.util.logging.Logger;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import moe.kyokobot.koe.Koe;
import moe.kyokobot.koe.KoeOptions;
import moe.kyokobot.koe.gateway.GatewayVersion;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyPair;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Server {
    private final List<ConnectedClient> connectedClients = new ObjectArrayList<>();
    private final NetworkServer networkServer;
    private final KeyPair keyPair = NetworkEncryptionUtils.createRsaKeyPair();

    private final ScheduledExecutorService scheduledExecutorService;

    private final ObjectArrayList<AudioTrack> trackPacketRegistry = new ObjectArrayList<>();
    private final Object2IntOpenHashMap<String> identifierToIdMap = new Object2IntOpenHashMap<>();
    private final Object2IntOpenHashMap<String> queryCache = new Object2IntOpenHashMap<>();

    private final int protocolVersion;
    private final int port;
    private final PacketRegistry registry;
    private final String apiKey;

    private final String serverName;
    private final String serverVersion;
    private final ServerFeatures features;
    private final String ytSourceVersion;
    private final String countryCode;
    private final ModdedInfo moddedInfo;
    private final YoutubeCipherH youtubeCipherH;

    private final DefaultAudioPlayerManager playerManager;
    private final Koe client;
    private final TrackCacheManager cacheManager;
    private final io.lolyay.discordmsend.server.cache.AudioCacheManager audioCacheManager;
    private final io.lolyay.discordmsend.server.upload.UploadDataManager uploadDataManager;
    private final io.lolyay.discordmsend.server.upload.S3UploadManager s3UploadManager;
    private final io.lolyay.discordmsend.server.upload.TrackUploadHandler trackUploadHandler;
    private final boolean singleGuildHQ;


    public Server(int port, int protocolVersion, PacketRegistry registry, ServerInitData initData, String apiKey){
        this.networkServer = new NetworkServer(port, registry, this);
        this.serverName = initData.serverName();
        this.serverVersion = initData.serverVersion();
        this.features = initData.features();
        this.ytSourceVersion = initData.ytSourceVersion();
        this.countryCode = initData.countryCode();
        this.moddedInfo = initData.moddedInfo();
        this.singleGuildHQ = initData.singleGuildHQ();
        this.port = port;
        this.registry = registry;
        this.protocolVersion = protocolVersion;
        this.apiKey = apiKey;
        if (singleGuildHQ) {
            Logger.info("========================================");
            Logger.info(" SINGLE GUILD HQ MODE ENABLED");
            Logger.info(" Using ALL resources for maximum quality");
            Logger.info("========================================");
            // HQ mode: Use more threads for better concurrency
            this.scheduledExecutorService = new ScheduledThreadPoolExecutor(6);
            ((ScheduledThreadPoolExecutor) scheduledExecutorService).setMaximumPoolSize(8);
            Logger.info("[HQ] Scheduler threads: 6 (max 8)");
        } else {
            // Normal mode: Balanced thread usage
            this.scheduledExecutorService = new ScheduledThreadPoolExecutor(3);
        }

        this.client = Koe.koe(KoeOptions.builder().setGatewayVersion(GatewayVersion.V8).create());
        this.playerManager = new DefaultAudioPlayerManager();
        this.cacheManager = new TrackCacheManager(this, singleGuildHQ ? 7 : 2); // HQ: 7 days, Normal: 2 days
        
        // Initialize audio cache manager
        try {
            this.audioCacheManager = new io.lolyay.discordmsend.server.cache.FileSystemAudioCacheManager("./cache/tracks");
            Logger.info("Audio cache initialized at ./cache/tracks");
        } catch (Exception e) {
            Logger.err("Failed to initialize audio cache: " + e.getMessage());
            throw new RuntimeException("Failed to initialize audio cache", e);
        }
        
        // Initialize upload managers
        try {
            this.uploadDataManager = new io.lolyay.discordmsend.server.upload.UploadDataManager("./cache/uploaddata.json");
            
            // Load S3 credentials from config
            String s3AccessKey = io.lolyay.discordmsend.server.config.ConfigFile.s3AccessKey;
            String s3SecretKey = io.lolyay.discordmsend.server.config.ConfigFile.s3SecretKey;
            String s3Region = io.lolyay.discordmsend.server.config.ConfigFile.s3Region;
            String bucketUrl = io.lolyay.discordmsend.server.config.ConfigFile.trackUploadBucketUrl;
            String publicUrl = io.lolyay.discordmsend.server.config.ConfigFile.publicDownloadBucketUrl;
            
            if (s3AccessKey == null || s3AccessKey.isEmpty() || 
                s3SecretKey == null || s3SecretKey.isEmpty() ||
                bucketUrl.isEmpty() || publicUrl.isEmpty()) {
                Logger.warn("S3 configuration incomplete. Upload functionality will be disabled.");
                Logger.warn("Configure s3AccessKey, s3SecretKey, trackUploadBucketUrl, and publicDownloadBucketUrl in config.yml");
                this.s3UploadManager = null;
                this.trackUploadHandler = null;
            } else {
                this.s3UploadManager = new io.lolyay.discordmsend.server.upload.S3UploadManager(
                    s3AccessKey, s3SecretKey, s3Region, bucketUrl, publicUrl);
                this.trackUploadHandler = new io.lolyay.discordmsend.server.upload.TrackUploadHandler(uploadDataManager, s3UploadManager);
                Logger.info("Track upload system initialized");
            }
        } catch (Exception e) {
            Logger.err("Failed to initialize upload managers: " + e.getMessage());
            throw new RuntimeException("Failed to initialize upload managers", e);
        }

        // Audio quality configuration
        playerManager.getConfiguration().setOutputFormat(StandardAudioDataFormats.DISCORD_OPUS);
        
        if (singleGuildHQ) {
            // MAXIMUM QUALITY SETTINGS - Use all available resources
            playerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.HIGH);
            playerManager.getConfiguration().setOpusEncodingQuality(10); // Maximum quality (0-10)
            playerManager.setFrameBufferDuration(10000); // 10s buffer for ultra-smooth playback
            playerManager.setTrackStuckThreshold(15000); // 15s before considering stuck
            playerManager.setPlayerCleanupThreshold(3600000); // 1 hour cleanup threshold
            
            // Enable all optimizations
            playerManager.enableGcMonitoring();
            
            Logger.info("[HQ] Frame buffer: 10000ms (ultra-smooth)");
            Logger.info("[HQ] Opus quality: 10/10 (maximum)");
            Logger.info("[HQ] Resampling: HIGH quality");
            Logger.info("[HQ] Stuck threshold: 15000ms");
        } else {
            // NORMAL QUALITY SETTINGS
            playerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.HIGH);
            playerManager.getConfiguration().setOpusEncodingQuality(10);
            playerManager.setFrameBufferDuration(5000); // 5s buffer
            playerManager.setTrackStuckThreshold(10000); // 10s stuck threshold
        }
        youtubeCipherH = new YoutubeCipherH();
        playerManager.registerSourceManager(youtubeCipherH.getSource());
        playerManager.setFrameBufferDuration(singleGuildHQ ? 10000 : 5000);
        playerManager.registerSourceManager(new YandexMusicAudioSourceManager(true));
        playerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        playerManager.registerSourceManager(new BandcampAudioSourceManager());
        playerManager.registerSourceManager(new VimeoAudioSourceManager());
        playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        playerManager.registerSourceManager(new BeamAudioSourceManager());
        playerManager.registerSourceManager(new GetyarnAudioSourceManager());
        playerManager.registerSourceManager(new NicoAudioSourceManager());
        playerManager.registerSourceManager(new HttpAudioSourceManager(MediaContainerRegistry.DEFAULT_REGISTRY));

        scheduleRepeating(() -> {
            // Timeout checker - iterate over copy to avoid ConcurrentModificationException
            List<Connection> connectionsCopy = new ArrayList<>(networkServer.getConnections());
            for(Connection connection : connectionsCopy){
                if(connection.getTimeSinceLastPacketRec() > getKeepAliveTimeout() * 10 * 1000L) {
                    connection.disconnect("Timeout");
                }
            }
        }, getKeepAliveTimeout() * 10, TimeUnit.SECONDS);

        scheduleRepeating(() -> {
            // Keep-alive updater - iterate over copy to avoid ConcurrentModificationException
            List<Connection> connectionsCopy = new ArrayList<>(networkServer.getConnections());
            for(Connection connection : connectionsCopy){
                if(connection.isActive() == false)
                    networkServer.removeConnection(connection);
                if(connection.getPhase() == NetworkPhase.POST_ENCRYPTION)
                    connection.send(new KeepAliveS2CPacket(System.currentTimeMillis()));
            }
        }, getKeepAliveTimeout(), TimeUnit.SECONDS);

        scheduleRepeating(() -> {
            // Client Updater - iterate over copy to avoid ConcurrentModificationException
            List<ConnectedClient> clientsCopy = new ArrayList<>(connectedClients);
            for(ConnectedClient connectedClient : clientsCopy){
                connectedClient.updateClient();
            }

        }, 500, TimeUnit.MILLISECONDS);

        scheduleRepeating(() -> {
            // Client Usage Updater - iterate over copy to avoid ConcurrentModificationException
            List<ConnectedClient> clientsCopy = new ArrayList<>(connectedClients);
            for(ConnectedClient connectedClient : clientsCopy){
                connectedClient.updateClientUsage();
            }

        }, 5, TimeUnit.SECONDS);

        // Cache expiration task - runs every 6 hours
        scheduleRepeating(() -> {
            cacheManager.expireOldTracks();
        }, 6, TimeUnit.HOURS);


    }

    public String getApiKey(){
        return apiKey;
    }

    public void removeClient(ConnectedClient connectedClient){
        this.connectedClients.remove(connectedClient);
    }

    public void removeClientByConnection(Connection connection){
        connectedClients.removeIf(connectedClient -> connectedClient.getConnection().equals(connection));
    }

    public List<ConnectedClient> getConnectedClients() {
        return connectedClients;
    }

    public AudioPlayerManager getPlayerManager() {
        return playerManager;
    }
    
    public io.lolyay.discordmsend.server.cache.AudioCacheManager getAudioCacheManager() {
        return audioCacheManager;
    }
    
    public io.lolyay.discordmsend.server.upload.TrackUploadHandler getTrackUploadHandler() {
        return trackUploadHandler;
    }

    public Koe getClient() {
        return client;
    }

    public void addConnectedClient(ConnectedClient connectedClient){
        connectedClients.add(connectedClient);
    }


    public void start() throws Exception{
        networkServer.start();
    }

    public CompletableFuture<MusicTrack> search(String q){
        CompletableFuture<MusicTrack> future = new CompletableFuture<>();


        if(queryCache.containsKey(q)){
            Logger.debug("Loaded from Cache " + q);
            int trackId = queryCache.get(q);
            markTrackAccessed(trackId);
            future.complete(MusicTrack.ofId(trackId));
            return future;
        }

        playerManager.loadItem(q, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack s) {
                trackPacketRegistry.add(s);
                int index = trackPacketRegistry.size() - 1;
                queryCache.put(q, index);
                identifierToIdMap.put(s.getIdentifier(), index);
                markTrackAccessed(index);
                Logger.debug("Loaded single!");
                future.complete(MusicTrack.ofId(index));
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                AudioTrack s = audioPlaylist.getTracks().get(0);
                trackPacketRegistry.add(s);
                int index = trackPacketRegistry.size() - 1;
                queryCache.put(q, index);
                identifierToIdMap.put(s.getIdentifier(), index);
                markTrackAccessed(index);
                future.complete(MusicTrack.ofId(index));
            }

            @Override
            public void noMatches() {
                future.complete(null);
            }

            @Override
            public void loadFailed(FriendlyException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public CompletableFuture<MusicTrack> addTrack(String q){
        CompletableFuture<MusicTrack> future = new CompletableFuture<>();


        if(queryCache.containsKey(q)){
            Logger.debug("Loaded from Cache " + q);
            int trackId = queryCache.get(q);
            markTrackAccessed(trackId);
            future.complete(MusicTrack.ofId(trackId));
            return future;
        }
        AudioTrack s = new NoOpAudioTrackHelper(q);

                trackPacketRegistry.add(s);
                int index = trackPacketRegistry.size() - 1;
                queryCache.put(q, index);
                identifierToIdMap.put(s.getIdentifier(), index);
                markTrackAccessed(index);
                Logger.debug("Loaded NO OP TRACK ( URL NON YT ) !");
                //TODO: Implement Lava-src or custom method to search track with image, length, title etc
                future.complete(MusicTrack.ofId(index));
        return future;
    }

    public CompletableFuture<List<MusicTrack>> searchMultiple(String q, int maxResults){
        CompletableFuture<List<MusicTrack>> future = new CompletableFuture<>();

        playerManager.loadItem(q, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack s) {
                List<MusicTrack> tracks = new ArrayList<>();
                trackPacketRegistry.add(s);
                int index = trackPacketRegistry.size() - 1;
                markTrackAccessed(index);
                tracks.add(MusicTrack.ofId(index));
                future.complete(tracks);
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                List<AudioTrack> list = audioPlaylist.getTracks().subList(0, Math.min(maxResults, audioPlaylist.getTracks().size()));
                List<MusicTrack> tracks = new ArrayList<>();
                for(AudioTrack track : list){
                    trackPacketRegistry.add(track);
                    int index = trackPacketRegistry.size() - 1;
                    markTrackAccessed(index);
                    tracks.add(MusicTrack.ofId(index));
                }
                future.complete(tracks);
            }

            @Override
            public void noMatches() {
                future.complete(null);
            }

            @Override
            public void loadFailed(FriendlyException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public AudioTrack resolveTo(MusicTrack musicTrack) {
        if (musicTrack == null) {
            return null;
        }

        int id = musicTrack.getTrackId();

        if (id < 0 || id >= trackPacketRegistry.size()) {
            return null;
        }
        
        markTrackAccessed(id);

        return trackPacketRegistry.get(id).makeClone();
    }

    public MusicTrack unResolve(AudioTrack audioTrack){
        int id = identifierToIdMap.get(audioTrack.getIdentifier());
        return MusicTrack.ofId(id);
    }

    public void schedule(Runnable runnable, long time, TimeUnit timeUnit){
        scheduledExecutorService.schedule(runnable, time, timeUnit);
    }

    public void scheduleRepeating(Runnable runnable, long time, TimeUnit timeUnit){
        scheduledExecutorService.scheduleAtFixedRate(runnable, time, time,timeUnit);
    }

    public int getEncryptionTimeout(){
        return 20;
    }

    public int getKeepAliveTimeout(){
        return 3;
    }

    public KeyPair getKeyPair(){
        return keyPair;
    }

    public List<ConnectedClient> connectedClients() {
        return connectedClients;
    }

    public NetworkServer networkServer() {
        return networkServer;
    }

    public KeyPair keyPair() {
        return keyPair;
    }

    public int protocolVersion() {
        return protocolVersion;
    }

    public int port() {
        return port;
    }

    public PacketRegistry registry() {
        return registry;
    }

    public String serverName() {
        return serverName;
    }

    public String serverVersion() {
        return serverVersion;
    }

    public ServerFeatures features() {
        return features;
    }

    public String ytSourceVersion() {
        return ytSourceVersion;
    }

    public String countryCode() {
        return countryCode;
    }

    public ModdedInfo moddedInfo() {
        return moddedInfo;
    }

    /**
     * Removes tracks from the server cache.
     * Called by TrackCacheManager when tracks expire.
     */
    public void removeTracksFromCache(List<Integer> trackIds) {
        for (int trackId : trackIds) {
            if (trackId >= 0 && trackId < trackPacketRegistry.size()) {
                AudioTrack track = trackPacketRegistry.get(trackId);
                if (track != null) {
                    identifierToIdMap.removeInt(track.getIdentifier());
                    trackPacketRegistry.set(trackId, null); // Clear but maintain index
                }
            }
        }
        
        // Remove from query cache (reverse lookup and remove)
        queryCache.entrySet().removeIf(entry -> trackIds.contains(entry.getValue()));
    }

    /**
     * Broadcasts a packet to all connected clients.
     */
    public void broadcastToAllClients(io.lolyay.discordmsend.network.protocol.packet.Packet<?> packet) {
        for (ConnectedClient client : connectedClients) {
            client.getConnection().send(packet);
        }
    }

    /**
     * Marks a track as accessed in the cache manager.
     */
    public void markTrackAccessed(int trackId) {
        cacheManager.markAccessed(trackId);
    }

}
