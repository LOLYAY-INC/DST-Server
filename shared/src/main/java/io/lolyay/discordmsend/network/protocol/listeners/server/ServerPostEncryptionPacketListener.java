package io.lolyay.discordmsend.network.protocol.listeners.server;


import io.lolyay.discordmsend.network.protocol.packet.PacketListener;
import io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc.*;

public interface ServerPostEncryptionPacketListener extends PacketListener {
    void onEncHello(EncHelloC2SPacket packet);
    void onKeepAlive(KeepAliveC2SPacket packet);
    void onDiscordDetails(DiscordDetailsC2SPacket packet);
    void onPlayerCreate(PlayerCreateC2SPacket packet);
    void onPlayerConnect(PlayerConnectC2SPacket packet);
    void onSearch(SearchC2SPacket packet);
    void onDetailsRequest(RequestTrackInfoC2SPacket packet);
    void onPlayTrack(PlayTrackC2SPacket packet);
    void onDefaultVolume(SetDefaultVolumeC2SPacket packet);
    void onPlayerSetVolume(PlayerSetVolumeC2SPacket packet);
    void onPlayerPause(PlayerPauseC2SPacket packet);
    void onPlayerResume(PlayerResumeC2SPacket packet);
    void onPlayerStop(PlayerStopC2SPacket packet);
    void onPing(PingC2SPacket packet);
    void onSearchMultiple(SearchMultipleC2SPacket packet);
    void onChangeSource(SetSourceC2SPacket packet);
    void onTrackInject(InjectTrackC2SPacket packet);
    void onRequestLink(RequestLinkC2SPacket packet);
    void onForceReconnect(ForceReconnectC2SPacket packet);
}
