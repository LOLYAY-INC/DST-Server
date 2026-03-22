package io.lolyay.discordmsend.network.protocol.listeners.server;


import io.lolyay.discordmsend.network.protocol.packet.PacketListener;
import io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc.*;
import io.lolyay.discordmsend.network.protocol.request.IRequestPacket;

public interface ServerPostEncryptionPacketListener extends PacketListener {
    void onEncHello(EncHelloC2SPacket packet);
    void onKeepAlive(KeepAliveC2SPacket packet);
    void onDiscordDetails(PlayerDiscordConnectC2SPacket packet);
    void onPlayTrack(PlayTrackC2SPacket packet);
    void onDefaultVolume(SetDefaultVolumeC2SPacket packet);
    void onPlayerSetVolume(PlayerSetVolumeC2SPacket packet);
    void onPlayerPause(PlayerPauseC2SPacket packet);
    void onPlayerResume(PlayerResumeC2SPacket packet);
    void onPlayerStop(PlayerStopC2SPacket packet);
    void onPing(PingC2SPacket packet);
    void onForceReconnect(ForceDiscordReconnectC2SPacket packet);
    void onSeek(SeekC2SPacket packet);
    void onRequest(IRequestPacket requestPacket);
}
