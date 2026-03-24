package io.lolyay.discordmsend.server;


import io.lolyay.discordmsend.network.protocol.Connection;
import io.lolyay.discordmsend.network.protocol.NetworkPhase;
import io.lolyay.discordmsend.network.protocol.encryption.NetworkEncryptionUtils;
import io.lolyay.discordmsend.network.protocol.packet.PacketRegistry;
import io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc.RequestTrackInfoC2SPacket;
import io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc.SearchMultipleC2SPacket;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.KeepAliveS2CPacket;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.SearchResponseS2CPacket;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.TrackDetailsS2CPacket;
import io.lolyay.discordmsend.network.protocol.request.SearchRequest;
import io.lolyay.discordmsend.network.protocol.request.TrackInfoRequest;
import io.lolyay.discordmsend.network.types.ModdedInfo;
import io.lolyay.discordmsend.network.types.ServerFeatures;
import io.lolyay.discordmsend.network.types.TrackMetadata;
import io.lolyay.discordmsend.obj.TrackId;
import io.lolyay.discordmsend.server.addon.AddonLoader;
import io.lolyay.discordmsend.server.addon.DstImplAddon;
import io.lolyay.discordmsend.server.cache.TrackCacheManager;
import io.lolyay.discordmsend.server.music.pools.opus.OpusEncoderPool;
import io.lolyay.discordmsend.server.music.pools.player.GuildPlayerPool;
import io.lolyay.discordmsend.server.music.providers.ProviderPool;
import io.lolyay.discordmsend.server.network.ConnectedClient;
import io.lolyay.discordmsend.server.network.NetworkServer;
import io.lolyay.discordmsend.server.network.ServerRequestManager;
import io.lolyay.eventbus.EventBus;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import moe.kyokobot.koe.Koe;
import moe.kyokobot.koe.KoeOptions;
import moe.kyokobot.koe.gateway.GatewayVersion;
import moe.kyokobot.koe.poller.udpqueue.QueueManagerPool;
import moe.kyokobot.koe.poller.udpqueue.UdpQueueFramePollerFactory;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

//TODO: holy giant file - slpit up
@Slf4j
@Getter
public class DstServer {
    public static final EventBus EVENT_BUS = new EventBus();

    private final List<DstImplAddon> addons;

    private final List<ConnectedClient> connectedClients = new ObjectArrayList<>();
    private final NetworkServer networkServer;
    private final KeyPair keyPair = NetworkEncryptionUtils.createRsaKeyPair();

    private final ScheduledExecutorService scheduledExecutorService;

    private final ObjectArrayList<TrackMetadata> trackPacketRegistry = new ObjectArrayList<>();
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

    private final Koe client;
    private final TrackCacheManager cacheManager;
    private final io.lolyay.discordmsend.server.cache.AudioCacheManager audioCacheManager;
    private final boolean singleGuildHQ;

    private final OpusEncoderPool opusEncoderPool;
    private final GuildPlayerPool guildPlayerPool;
    private final int opusEncoderPoolSize = 2;
    private final int opusQueueLen = 200;


    public DstServer(int port, int protocolVersion, PacketRegistry registry, ServerInitData initData, String apiKey){
        this.networkServer = new NetworkServer(port, registry, this);
        this.serverName = initData.getServerName();
        this.serverVersion = initData.getServerVersion();
        this.features = initData.getFeatures();
        this.ytSourceVersion = initData.getServerId();
        this.countryCode = initData.getCountryCode();
        this.singleGuildHQ = initData.isSingleGuildHQ();
        this.port = port;
        this.registry = registry;
        this.protocolVersion = protocolVersion;
        this.apiKey = apiKey;
        this.scheduledExecutorService = new ScheduledThreadPoolExecutor(6);
        ((ScheduledThreadPoolExecutor) scheduledExecutorService).setMaximumPoolSize(8);
        this.client = Koe.koe(KoeOptions.builder().setFramePollerFactory(new UdpQueueFramePollerFactory(new QueueManagerPool(Runtime.getRuntime().availableProcessors(), 2000))).setGatewayVersion(GatewayVersion.V8).create());
        this.cacheManager = new TrackCacheManager(this, singleGuildHQ ? 7 : 2);


        this.opusEncoderPool = new OpusEncoderPool(this.opusEncoderPoolSize, this.opusQueueLen);
        this.guildPlayerPool = new GuildPlayerPool(4);
        
        // Initialize audio cache manager
        try {
            this.audioCacheManager = new io.lolyay.discordmsend.server.cache.FileSystemAudioCacheManager("./cache/tracks");
            log.info("Audio cache initialized at ./cache/tracks");
        } catch (Exception e) {
            log.error("Failed to initialize audio cache: " + e.getMessage());
            throw new RuntimeException("Failed to initialize audio cache", e);
        }



        scheduleRepeating(() -> {
            List<Connection> connectionsCopy = new ArrayList<>(networkServer.getConnections());
            for(Connection connection : connectionsCopy){
                if(connection.getTimeSinceLastPacketRec() > getKeepAliveTimeout() * 10 * 1000L) {
                    connection.disconnect("Timeout");
                }
            }
        }, getKeepAliveTimeout() * 10L, TimeUnit.SECONDS);

        scheduleRepeating(() -> {
            List<Connection> connectionsCopy = new ArrayList<>(networkServer.getConnections());
            for(Connection connection : connectionsCopy){
                if(!connection.isActive())
                    networkServer.removeConnection(connection);
                if(connection.getPhase() == NetworkPhase.POST_ENCRYPTION)
                    connection.send(new KeepAliveS2CPacket(System.currentTimeMillis()));
            }
        }, getKeepAliveTimeout(), TimeUnit.SECONDS);

        scheduleRepeating(() -> {
            List<ConnectedClient> clientsCopy = new ArrayList<>(connectedClients);
            for(ConnectedClient connectedClient : clientsCopy){
                connectedClient.updateClient();
            }

        }, 500, TimeUnit.MILLISECONDS);

        scheduleRepeating(() -> {
            List<ConnectedClient> clientsCopy = new ArrayList<>(connectedClients);
            for(ConnectedClient connectedClient : clientsCopy){
                connectedClient.updateClientUsage();
            }

        }, 5, TimeUnit.SECONDS);

        scheduleRepeating(cacheManager::expireOldTracks, 6, TimeUnit.HOURS);

        // Register Server Requests
        ServerRequestManager.registerExchange(SearchRequest.EXCHANGE_TYPE, (requestPacket, server, client) -> {
            if(!(requestPacket instanceof SearchMultipleC2SPacket searchMultipleC2SPacket))
                return null;
            List<TrackMetadata> metadata = ProviderPool.getInstance().search(searchMultipleC2SPacket.query(), searchMultipleC2SPacket.maxResults());
            IntArrayList returns = new IntArrayList();

            Int2ObjectArrayMap<TrackMetadata> tracks = new Int2ObjectArrayMap<>();
            for (TrackMetadata track : metadata) {
                int index = trackPacketRegistry.size();
                TrackMetadata t = track.withId(index);
                trackPacketRegistry.add(t);
                markTrackAccessed(index);
                identifierToIdMap.put(t.identifier(), index);
                tracks.put(index, t);
                returns.add(index);
            }

            if(searchMultipleC2SPacket.details())
                tracks.forEach((t,m) -> client.sendPacket(new TrackDetailsS2CPacket(
                        m
                )));
            return new SearchResponseS2CPacket(returns, searchMultipleC2SPacket.sequence());
        });

        ServerRequestManager.registerExchange(TrackInfoRequest.EXCHANGE_TYPE, (requestPacket, server, client) -> {
            if (!(requestPacket instanceof RequestTrackInfoC2SPacket trackInfoRequest))
                return null;
            TrackMetadata trackMetadata = resolveTo(TrackId.ofId(trackInfoRequest.trackId()));
            return new TrackDetailsS2CPacket(
                    trackMetadata
            );
        });

        addons = new AddonLoader().load(this);

    }

    public void removeClient(ConnectedClient connectedClient){
        this.connectedClients.remove(connectedClient);
    }

    public void removeClientByConnection(Connection connection){
        connectedClients.removeIf(connectedClient -> connectedClient.getConnection().equals(connection));
    }

    public void addConnectedClient(ConnectedClient connectedClient){
        connectedClients.add(connectedClient);
    }

    public ModdedInfo genModdedInfo() {
        return new ModdedInfo(
                this.getAddons().stream()
                        .filter(addon -> addon.getId() != null && addon.getVersion() != null)
                        .collect(java.util.stream.Collectors.toMap(
                                DstImplAddon::getId,
                                DstImplAddon::getVersion,
                                (existing, replacement) -> existing
                        ))
        );

    }

    public void start() throws Exception {
        addons.forEach(addon -> {
            log.info("Enabling {} v{}", addon.getId(), addon.getVersion());
            try {
                addon.onEnable(this);
                addon.getProviders().forEach(provider -> {
                    ProviderPool.getInstance().register(provider.providerFactory(), provider.playablePredicate());
                });
                addon.getSearchers().forEach(searcher -> ProviderPool.getInstance().register(searcher));

            } catch (Exception e) {
                log.error("Failed to enable addon {}: {}", addon.getId(), e.getMessage(), e);
            }
            log.info("Successfully Enabled addon {}", addon.getId());
        });
        networkServer.start();
    }

    public TrackMetadata resolveTo(TrackId trackId) {
        if (trackId == null) {
            return null;
        }
        int id = trackId.getTrackId();
        if (id < 0 || id >= trackPacketRegistry.size()) {
            return null;
        }
        markTrackAccessed(id);
        return trackPacketRegistry.get(id);
    }

    public TrackId unResolve(TrackMetadata audioTrack){
        int id = identifierToIdMap.get(audioTrack.identifier());
        return TrackId.ofId(id);
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


    public void removeTracksFromCache(List<Integer> trackIds) {
        for (int trackId : trackIds) {
            if (trackId >= 0 && trackId < trackPacketRegistry.size()) {
                TrackMetadata track = trackPacketRegistry.get(trackId);
                if (track != null) {
                    identifierToIdMap.removeInt(track.identifier());
                    trackPacketRegistry.set(trackId, null); // Clear but maintain index
                }
            }
        }
        
        queryCache.entrySet().removeIf(entry -> trackIds.contains(entry.getValue()));
    }

    public void broadcastToAllClients(io.lolyay.discordmsend.network.protocol.packet.Packet<?> packet) {
        for (ConnectedClient client : connectedClients) {
            client.getConnection().send(packet);
        }
    }

    public void markTrackAccessed(int trackId) {
        cacheManager.markAccessed(trackId);
    }

}
