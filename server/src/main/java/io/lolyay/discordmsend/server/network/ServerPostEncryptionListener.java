package io.lolyay.discordmsend.server.network;

import io.lolyay.discordmsend.network.protocol.Connection;
import io.lolyay.discordmsend.network.protocol.listeners.server.ServerPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc.*;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.*;
import io.lolyay.discordmsend.network.protocol.request.IRequestPacket;
import io.lolyay.discordmsend.obj.CUserData;
import io.lolyay.discordmsend.obj.TrackId;
import io.lolyay.discordmsend.server.DstServer;
import io.lolyay.discordmsend.server.event.PostClientConnectEvent;
import io.lolyay.discordmsend.server.event.PreClientConnectEvent;
import io.lolyay.discordmsend.server.music.players.GuildPlayerInstance;
import io.lolyay.discordmsend.server.music.consumers.DiscordTrackConsumer;
import lombok.extern.slf4j.Slf4j;
import moe.kyokobot.koe.VoiceServerInfo;

import static io.lolyay.discordmsend.server.DstServer.EVENT_BUS;

@Slf4j
public class ServerPostEncryptionListener implements ServerPostEncryptionPacketListener {
    private final DstServer dstServer;
    private final Connection connection;
    private final ConnectedClient client;

    public ServerPostEncryptionListener(DstServer dstServer, Connection connection, ConnectedClient client) {
        this.dstServer = dstServer;
        this.connection = connection;
        this.client = client;
        PreClientConnectEvent ev = EVENT_BUS.postAndGet(new PreClientConnectEvent(
                dstServer,
                client,
                dstServer.genModdedInfo()
                ));

        if(ev.isCancelled()) {
            connection.disconnect(ev.getCR() != null ? ev.getCR() : "Connection cancelled by server.");
            return;
        }

        connection.send(new EncHelloS2CPacket(
                dstServer.serverName(),
                dstServer.serverVersion(),
                dstServer.protocolVersion(),
                dstServer.features(),
                dstServer.ytSourceVersion(),
                ev.getModdedInfo(),
                dstServer.countryCode()
        ));
        log.debug("Sent EncHelloS2CPacket");
    }

    @Override
    public void onEncHello(EncHelloC2SPacket packet) {
        log.info("Client \"%s\"s by \"%s\" Version \"%s\" Connected with ClientId %s".formatted(packet.userAgent(), packet.userAuthor(), packet.userVersion(), dstServer.getConnectedClients().size()));

        client.setUserData(new CUserData(packet.userAgent(),
                packet.userVersion(), packet.userAuthor(),
                packet.features()));
        client.setUserId(packet.botId());
        PostClientConnectEvent ev = EVENT_BUS.postAndGet(new PostClientConnectEvent(
                dstServer,
                client,
                packet
        ));

        if (ev.isCancelled()) {
            connection.disconnect(ev.getCR() != null ? ev.getCR() : "Connection cancelled by server.");
        }
    }

    @Override
    public void onKeepAlive(KeepAliveC2SPacket packet) {
        getConnection().timeSinceLastPacket = System.currentTimeMillis(); // <-- update here
    }

    @Override
    public void onDiscordDetails(PlayerDiscordConnectC2SPacket packet) {
        log.info("Discord Connect for " + client.getUserData().userAgent());
        ((DiscordTrackConsumer) client.getPlayer().getOrCreatePlayer(packet.guildId())
                        .getConsumer()).getConnection().connect(VoiceServerInfo.builder()
                .setChannelId(packet.channelId())
                .setEndpoint(packet.endPoint())
                .setSessionId(packet.sessionId())
                .setToken(packet.token())
                .build()
        );
    }

    @Override
    public void onPlayTrack(PlayTrackC2SPacket packet) {
        client.getPlayer().getOrCreatePlayer(packet.guildId()).playTrack(dstServer.resolveTo(TrackId.ofId(packet.trackId())));
    }

    @Override
    public void onDefaultVolume(SetDefaultVolumeC2SPacket packet) {
        client.getPlayer().setDefaultVolume((float) packet.volume() / 100);
    }

    @Override
    public void onDisconnect(String reason) {
        log.info("Client disconnected, {}", reason);
        dstServer.networkServer().removeConnection(connection);
    }

    @Override
    public Connection getConnection() {
        return connection;
    }


    @Override
    public void onPing(PingC2SPacket packet) {
        getConnection().send(new PongS2CPacket(packet.data(), System.currentTimeMillis()));
        log.debug("Pong!");
    }


    @Override
    public void onPlayerPause(PlayerPauseC2SPacket packet) {
        client.getPlayer().getOrCreatePlayer(packet.guildId()).pause();
    }

    @Override
    public void onPlayerResume(PlayerResumeC2SPacket packet) {
        client.getPlayer().getOrCreatePlayer(packet.guildId()).resume();
    }

    @Override
    public void onPlayerStop(PlayerStopC2SPacket packet) {
        client.getPlayer().getOrCreatePlayer(packet.guildId()).stop();
    }

    @Override
    public void onPlayerSetVolume(PlayerSetVolumeC2SPacket packet) {
        client.getPlayer().getOrCreatePlayer(packet.guildId()).setVolume((float) packet.volume() / 100);
    }
//
//    @Override
//    public void onRequestLink(RequestLinkC2SPacket packet) {
//        long guildId = packet.guildId();
//        int sequence = packet.sequence();
//
//        // Check if upload system is initialized
//        if (server.getTrackUploadHandler() == null) {
//            connection.send(LinkResponseS2CPacket.error(guildId, sequence,
//                "Upload system not initialized. S3 credentials may be missing."));
//            log.warn("Link request failed: Upload system not initialized");
//            return;
//        }
//
//        try {
//            TrackPlayerInstance player = client.getPlayer().getOrCreatePlayer(guildId);
//
//            // Check if a track is playing
//            if (player.getConsumer().getStreamer().getPlayingTrack() == null) {
//                connection.send(LinkResponseS2CPacket.error(guildId, sequence,
//                    "No track currently playing"));
//                log.debug("Link request failed: No track playing");
//                return;
//            }
//
//            String trackUri = player.getConsumer().getStreamer().getPlayingTrack().getUniqueId();
//            String cacheId = server.getAudioCacheManager().computeHash(trackUri);
//
//            // Check if PCM file exists
//            if (!server.getAudioCacheManager().hasTrack(trackUri)) {
//                connection.send(LinkResponseS2CPacket.error(guildId, sequence,
//                    "Track not cached. Please wait for track to finish playing first."));
//                log.debug("Link request failed: Track not cached for " + cacheId);
//                return;
//            }
//
//            // Get the PCM file path
//            String pcmFilePath = "./cache/tracks/" + cacheId + ".pcm";
//
//            log.debug("Processing link request for cache ID: " + cacheId + " (sequence: " + sequence + ")");
//
//            // Start upload process (or get cached URL)
//            server.getTrackUploadHandler().getOrUploadTrack(cacheId, pcmFilePath)
//                .thenAccept(url -> {
//                    connection.send(LinkResponseS2CPacket.success(guildId, sequence, url));
//                    log.debug("Link request successful: " + url);
//                })
//                .exceptionally(ex -> {
//                    connection.send(LinkResponseS2CPacket.error(guildId, sequence,
//                        "Upload failed: " + ex.getMessage()));
//                    log.err("Link request upload failed: " + ex.getMessage());
//                    return null;
//                });
//
//        } catch (Exception e) {
//            connection.send(LinkResponseS2CPacket.error(guildId, sequence,
//                "Internal error: " + e.getMessage()));
//            log.err("Link request error: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }

    @Override
    public void onForceReconnect(ForceDiscordReconnectC2SPacket packet) {
        GuildPlayerInstance player = client.getPlayer().getOrCreatePlayer(packet.guildId());
        
        // Check if this is a Discord connection
        if (player.getConsumer() instanceof DiscordTrackConsumer dcConsumer) {
            dcConsumer.getConnection().stopAudioFramePolling();
            dcConsumer.getConnection().reconnect();
            dcConsumer.getConnection().startAudioFramePolling();
            log.debug("Force reconnected Discord player for guild {}", packet.guildId());
        } else {
            log.warn("Force reconnect requested for non-Discord player (guild {}). Ignoring.", packet.guildId());
            //This is possible to implement ( restart generator and sender thread and clear pregen buffer but why? It shouldnt break unlike discord)
        }
    }

    @Override
    public void onSeek(SeekC2SPacket packet) {
        client.getPlayer().getOrCreatePlayer(packet.guildId()).seek(packet.positionMs());
    }

    @Override
    public void onRequest(IRequestPacket requestPacket) {
        ServerRequestManager.getExchange(requestPacket.getExchangeType())
                .handle(requestPacket, dstServer, client);
    }
}
