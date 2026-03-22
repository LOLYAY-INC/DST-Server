package io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc;

import io.lolyay.discordmsend.network.protocol.listeners.client.ClientPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;

public record PongS2CPacket(
        long clientSentTimeMs,
        long serverTimeMs
) implements Packet<ClientPostEncryptionPacketListener> {

    public static final PacketCodec<PongS2CPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeLong(packet.clientSentTimeMs());
                buf.writeLong(packet.serverTimeMs());
            },
            // Decoder
            (buf) -> new PongS2CPacket(
                    buf.readLong(),
                    buf.readLong()
            )
    );

    @Override
    public void apply(ClientPostEncryptionPacketListener listener) {
        listener.onPong(this);
    }
}