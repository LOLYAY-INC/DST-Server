package io.lolyay.discordmsend.network.protocol.listeners.client;


import io.lolyay.discordmsend.network.protocol.packet.PacketListener;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.preenc.EncryptionRequestS2CPacket;

public interface ClientPreEncryptionPacketListener extends PacketListener {
    void onEncryptionRequest(EncryptionRequestS2CPacket packet);
}