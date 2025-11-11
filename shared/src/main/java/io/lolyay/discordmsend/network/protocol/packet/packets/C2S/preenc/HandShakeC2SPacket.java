package io.lolyay.discordmsend.network.protocol.packet.packets.C2S.preenc;


import io.lolyay.discordmsend.network.protocol.listeners.server.ServerPreEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;


public record HandShakeC2SPacket(int protocolVersion, String serverAddress, String apiKey, int serverPort ) implements Packet<ServerPreEncryptionPacketListener> {

    public static final PacketCodec<HandShakeC2SPacket> CODEC = PacketCodec.create(
            (buf, packet) -> {
                buf.writeVarInt(packet.protocolVersion);
                buf.writeString(packet.serverAddress);
                buf.writeString(packet.apiKey);
                buf.writeShort(packet.serverPort);
            },
            (buf) -> new HandShakeC2SPacket(
                    buf.readVarInt(),
                    buf.readString(255),
                    buf.readString(255),
                    buf.readShort()
            )
    );

    @Override
    public void apply(ServerPreEncryptionPacketListener listener) {
        listener.onHandShake(this);
    }
}