package io.lolyay.discordmsend.network.protocol.listeners.server;


import io.lolyay.discordmsend.network.protocol.packet.PacketListener;
import io.lolyay.discordmsend.network.protocol.packet.packets.C2S.preenc.EncryptionResponseC2SPacket;
import io.lolyay.discordmsend.network.protocol.packet.packets.C2S.preenc.HandShakeC2SPacket;

public interface ServerPreEncryptionPacketListener extends PacketListener {
    void onHandShake(HandShakeC2SPacket packet);
    void onEncryptionResponse(EncryptionResponseC2SPacket packet);
}