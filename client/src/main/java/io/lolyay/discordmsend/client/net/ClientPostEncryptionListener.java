package io.lolyay.discordmsend.client.net;

import io.lolyay.discordmsend.client.DstClient;
import io.lolyay.discordmsend.client.PlayerStatus;
import io.lolyay.discordmsend.client.ServerStatus;
import io.lolyay.discordmsend.network.Enviroment;
import io.lolyay.discordmsend.network.protocol.Connection;
import io.lolyay.discordmsend.network.protocol.listeners.client.ClientPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc.EncHelloC2SPacket;
import io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc.KeepAliveC2SPacket;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.*;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.events.*;
import io.lolyay.discordmsend.network.protocol.request.IResponsePacket;
import io.lolyay.discordmsend.obj.CUserData;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientPostEncryptionListener implements ClientPostEncryptionPacketListener {
    private final Connection connection;
    private final DstClient dstClient;

    public ClientPostEncryptionListener(Connection connection, DstClient dstClient) {
        this.connection = connection;
        log.info("Waiting for EncHello...");
        this.dstClient = dstClient;
    }


    @Override
    public void onDisconnect(String reason) {
        log.debug("Client: Disconnected during post encrpytion: {}", reason);
        dstClient.getEventHandler().onDisconnect(reason);
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public void onEncHello(EncHelloS2CPacket packet) {
        if(Enviroment.PROTOCOL_VERSION != packet.serverProtocolId()){
            log.error("Protocol version missmatch: CLIENT: {} | SERVER: {}", Enviroment.PROTOCOL_VERSION, packet.serverProtocolId());
            getConnection().disconnect("Protocol Version missmatch!");
            throw new RuntimeException("Protocol version missmatch");
        }

        CUserData userData = dstClient.getUserData();
        connection.send(new EncHelloC2SPacket(userData.userAgent(), userData.userVersion(), userData.userAuthor(), userData.features(), dstClient.getDiscordUserId()));
        log.info("Connected to Server \"%s\". Server Version: \"%s\". YT-Source Version (Search): %s. Server Locale: %s".formatted(packet.serverName(), packet.serverVersion(), packet.ytSourceVersion(), packet.countryCode()));
        log.info("Server Features: {}", packet.features());
        log.info("Server Plugins: {}", packet.moddedInfo());
    }

    @Override
    public void onKeepAlive(KeepAliveS2CPacket packet) {
        getConnection().send(new KeepAliveC2SPacket(packet.keepAliveId()));
    }

    @Override
    public void onTrackDetails(TrackDetailsS2CPacket packet) {
        dstClient.onTrackInfo(packet);
        if (dstClient.getRequestManager().handleResponse(packet)) {
            return;
        }
    }

    @Override
    public void onStatistics(StatisticsS2CPacket packet) {
        dstClient.setServerStatus(new ServerStatus(packet.maxMem(), packet.freeMem(), packet.cpuUsageP(), packet.players(), packet.clients()));
    }

    @Override
    public void onPlayerUpdate(PlayerUpdateS2CPacket packet) {
        dstClient.setPlayerStatus(new PlayerStatus(packet.playerId(), packet.paused(), packet.volume(), packet.position(), packet.hasTrack(), packet.trackId()));
    }

    @Override
    public void onPong(PongS2CPacket packet) {
        dstClient.setPongReceived(packet.clientSentTimeMs(), packet.serverTimeMs());
    }

    @Override
    public void onPlayerPause(PlayerPauseS2CPacket packet) {
        dstClient.getEventHandler().onPlayerPause(packet);
    }

    @Override
    public void onPlayerResume(PlayerResumeS2CPacket packet) {
        dstClient.getEventHandler().onPlayerResume(packet);
    }

    @Override
    public void onPlayerTrackStart(PlayerTrackStartS2CPacket packet) {
        dstClient.getEventHandler().onPlayerTrackStart(packet);
    }

    @Override
    public void onPlayerTrackEnd(PlayerTrackEndS2CPacket packet) {
        dstClient.getEventHandler().onPlayerTrackEnd(packet);
    }

    @Override
    public void onPlayerTrackError(PlayerTrackFailS2CPacket packet) {
        dstClient.getEventHandler().onPlayerTrackError(packet);
    }

    @Override
    public void onPlayerTrackStuck(PlayerTrackStuckS2CPacket packet) {
        dstClient.getEventHandler().onPlayerTrackStuck(packet);
    }

    @Override
    public void onResponse(IResponsePacket packet) {
        dstClient.getRequestManager().handleResponse(packet);
    }

    @Override
    public void onCacheExpire(CacheExpireS2C packet) {
        dstClient.onCacheExpire(packet);
    }

    @Override
    public void onTrackTimingUpdate(TrackTimingUpdateS2CPacket packet) {
        dstClient.getEventHandler().onTrackTimingUpdate(packet);
    }

    @Override
    public void onAudio(AudioS2CPacket packet) {
        dstClient.getEventHandler().onAudio(packet);
    }
}
