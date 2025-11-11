package io.lolyay.discordmsend.network.protocol.listeners.client;


import io.lolyay.discordmsend.network.protocol.packet.PacketListener;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.*;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.ev.*;

public interface ClientPostEncryptionPacketListener extends PacketListener {
    void onEncHello(EncHelloS2CPacket packet);
    void onKeepAlive(KeepAliveS2CPacket packet);
    void onTrackDetails(TrackDetailsS2CPacket packet);
    void onSearchResponse(TrackSearchResponseS2CPacket packet);
    void onStatistics(StatisticsS2CPacket packet);
    void onPlayerUpdate(PlayerUpdateS2CPacket packet);
    void onPong(PongS2CPacket packet);
    void onSearchMultiple(SearchMultipleResponseS2CPacket packet);
    void onCacheExpire(CacheExpireS2C packet);
    void onLinkResponse(LinkResponseS2CPacket packet);
    void onTrackTimingUpdate(TrackTimingUpdateS2CPacket packet);

    //Events
    void onPlayerPause(PlayerPauseS2CPacket packet);
    void onPlayerResume(PlayerResumeS2CPacket packet);
    void onPlayerTrackStart(PlayerTrackStartS2CPacket packet);
    void onPlayerTrackEnd(PlayerTrackEndS2CPacket packet);
    void onPlayerTrackError(PlayerTrackFailS2CPacket packet);
    void onPlayerTrackStuck(PlayerTrackStuckS2CPacket packet);



    void onAudio(AudioS2CPacket audioS2CPacket);
}
