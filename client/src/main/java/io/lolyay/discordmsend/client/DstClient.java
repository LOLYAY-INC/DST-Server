package io.lolyay.discordmsend.client;

import io.lolyay.discordmsend.client.net.NetworkingClient;
import io.lolyay.discordmsend.client.net.ClientRequestManager;
import io.lolyay.discordmsend.network.Enviroment;
import io.lolyay.discordmsend.network.protocol.NetworkPhase;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketRegistry;
import io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc.*;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.*;
import io.lolyay.discordmsend.network.protocol.request.*;
import io.lolyay.discordmsend.network.types.ClientFeatures;
import io.lolyay.discordmsend.network.types.TrackMetadata;
import io.lolyay.discordmsend.obj.CUserData;
import io.lolyay.discordmsend.obj.TrackId;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DstClient {
    private final static PacketRegistry packetRegistry = new PacketRegistry();

    {
        try {
            packetRegistry.registerAll();
            log.debug("Registered All packets");
        } catch (Exception e) {
            log.error("Couldn't Register Packets, ");
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Getter
    private final ClientRequestManager requestManager = new ClientRequestManager();
    @Getter
    private final Map<Long, PlayerStatus> playerStatusMap = new ConcurrentHashMap<>();
    private final Map<Integer, TrackMetadata> id2TrackInfoCache = new ConcurrentHashMap<>();
    private NetworkingClient client;

    private float pingMs;
    @Getter
    private long clockOffsetMs;

    @Getter
    private final long discordUserId;
    @Getter
    private final String apiKey;
    @Getter
    private final CUserData userData;
    @Getter
    private final ClientEventHandler eventHandler;


    @Getter
    private ServerStatus status;

    public static DstClient createDiscord(long discordUserId, @Nullable String apiKey, CUserData cUserData, ClientEventHandler eventHandler) {
        cUserData.features().enable(ClientFeatures.Feature.IS_DISCORD_BOT);
        return new DstClient(discordUserId, apiKey, cUserData, eventHandler);
    }

    public static DstClient createDirect(@Nullable String apiKey, CUserData cUserData, ClientEventHandler eventHandler) {
        cUserData.features().disable(ClientFeatures.Feature.IS_DISCORD_BOT);
        return new DstClient(-1, apiKey, cUserData, eventHandler);
    }

    public <T extends Packet<?> & IRequestPacket,
            R extends IResponsePacket> CompletableFuture<R> sendRequest(IRequest<T, R> request) {
        T packet = requestManager.createAndRegister(request);
        client.connection.send(packet);
        return request.future();
    }

    public void forceDiscordReconnect(long guildId) {
        client.connection.send(new ForceDiscordReconnectC2SPacket(guildId));
    }

    @ApiStatus.Internal
    public void setPongReceived(long clientSentTimeMs, long serverTimeMs) {
        long clientRecvTimeMs = System.currentTimeMillis();
        long rttMs = clientRecvTimeMs - clientSentTimeMs;
        this.pingMs = (float) rttMs / 2;

        long midpointMs = (clientSentTimeMs + clientRecvTimeMs) / 2;
        this.clockOffsetMs = serverTimeMs - midpointMs;
    }

    public void setServerStatus(ServerStatus status) {
        this.status = status;
    }

    public ServerStatus getServerStatus() {
        return status;
    }

    public float getPing() {
        return pingMs;
    }

    public void setPlayerStatus(PlayerStatus status) {
        playerStatusMap.put(status.guildId(), status);
    }

    public PlayerStatus getPlayerStatus(long guildId) {
        return playerStatusMap.get(guildId);
    }


    public void disconnect(String reason) {
        if (client != null) {
            client.disconnect(reason);
            client = null;
        }
        clearCache();
    }

    public void clearCache() {
        id2TrackInfoCache.clear();
    }

    public void connect(String host, int port) {
        disconnect("Reconnecting...");
        Thread networkThread = new Thread(() -> {
            try {
                this.client = new NetworkingClient(Enviroment.PROTOCOL_VERSION, host, port, packetRegistry, this);
                this.client.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "DST-NetworkThread");
        networkThread.setDaemon(true);
        networkThread.start();
    }

    public void connectToDiscordServer(long guildId, String sessionId, String endpoint, String token, long channelId) {
        client.connection.send(new PlayerDiscordConnectC2SPacket(guildId, endpoint, sessionId, token, channelId));
    }

    public CompletableFuture<TrackMetadata> getTrackInfo(TrackId track) {
        int trackId = track.getTrackId();
        if (id2TrackInfoCache.containsKey(trackId)) {
            return CompletableFuture.completedFuture(id2TrackInfoCache.get(trackId));
        }

        return sendRequest(new TrackInfoRequest(trackId)).thenApply(packet -> {
            id2TrackInfoCache.put(packet.metadata().id(), packet.metadata());
            return packet.metadata();
        });
    }

    public CompletableFuture<List<TrackId>> searchTracksMultiple(String query, int maxResults) {
        return sendRequest(new SearchRequest(query, true, maxResults))
                .thenApply(packet -> packet.trackIds().stream().map(TrackId::ofId).toList());
    }


    public CompletableFuture<String> requestLinkForCurrentSong(long guildId) {
        return sendRequest(new LinkRequest(guildId)).thenApply(packet -> {
            if (packet.success()) {
                log.info("Link request successful: {}", packet.link());
                return packet.link();
            }
            log.error("Link request failed: {}", packet.errorMessage());
            throw new CompletionException(new RuntimeException(packet.errorMessage()));
        });
    }

    public void playTrack(TrackId track, long guildId) {
        client.connection.send(new PlayTrackC2SPacket(track.getTrackId(), guildId));
    }

    public void seek(long position, long guildId) {
        client.connection.send(new SeekC2SPacket(guildId, position));
    }

    public void setDefaultVolume(int volume) {
        client.connection.send(new SetDefaultVolumeC2SPacket(volume));
    }

    @ApiStatus.Internal
    public void onLinkResponse(LinkResponseS2CPacket packet) {
        log.warn("Received unhandled link response (seq: {})", packet.sequence());
    }


    @ApiStatus.Internal
    public void onTrackInfo(TrackDetailsS2CPacket packet) {
        id2TrackInfoCache.put(packet.metadata().id(), packet.metadata());
    }


    @ApiStatus.Internal
    public void onCacheExpire(CacheExpireS2C packet) {
        for (int trackId : packet.expiredTrackIds()) {
            id2TrackInfoCache.remove(trackId);
        }
    }


    @Blocking
    public void waitUntilConnected() {
        while (client == null || client.connection == null || client.connection.getPhase() != NetworkPhase.POST_ENCRYPTION) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void setVolume(long guildId, int volume) {
        client.connection.send(new PlayerSetVolumeC2SPacket(guildId, volume));
    }

    public void pause(long guildId) {
        client.connection.send(new PlayerPauseC2SPacket(guildId));
    }

    public void resume(long guildId) {
        client.connection.send(new PlayerResumeC2SPacket(guildId));
    }

    public void stop(long guildId) {
        client.connection.send(new PlayerStopC2SPacket(guildId));
    }

    public void ping() {
        client.connection.send(new PingC2SPacket(System.currentTimeMillis()));
        //TODO: make this ping automatically!
    }

}
