package io.lolyay.discordmsend.network.protocol.packet.packets.C2S.preenc;


import io.lolyay.discordmsend.network.protocol.listeners.server.ServerPreEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;

public record EncryptionResponseC2SPacket(byte[] sharedSecret) implements Packet<ServerPreEncryptionPacketListener> {

    public static final PacketCodec<EncryptionResponseC2SPacket> CODEC = PacketCodec.create(
            (buf, packet) -> {
                buf.writeBytes(packet.sharedSecret);
            },
            (buf) -> new EncryptionResponseC2SPacket(
                    buf.IreadBytes(128)
            )
    );

    @Override
    public void apply(ServerPreEncryptionPacketListener listener) {
        listener.onEncryptionResponse(this);
    }
}