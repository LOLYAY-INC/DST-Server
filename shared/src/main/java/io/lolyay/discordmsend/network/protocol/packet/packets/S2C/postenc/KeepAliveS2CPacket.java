package io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc;


import io.lolyay.discordmsend.network.protocol.listeners.client.ClientPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;

public record KeepAliveS2CPacket(
        long keepAliveId
) implements Packet<ClientPostEncryptionPacketListener> {
    /**
     * The codec for the entire LoginSuccessS2CPacket.
     */
    public static final PacketCodec<KeepAliveS2CPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeLong(packet.keepAliveId());
            },
            // Decoder
            (buf) -> {
                return new KeepAliveS2CPacket(buf.readLong());
            }
    );

    @Override
    public void apply(ClientPostEncryptionPacketListener listener) {
        listener.onKeepAlive(this);
    }
}