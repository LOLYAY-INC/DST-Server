package io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc;


import io.lolyay.discordmsend.network.protocol.listeners.server.ServerPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;

public record PingC2SPacket(
        long data

) implements Packet<ServerPostEncryptionPacketListener> {

    public static final PacketCodec<PingC2SPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeLong(packet.data);
            },
            // Decoder
            (buf) -> new PingC2SPacket(
                    buf.readLong()
            )
    );

    @Override
    public void apply(ServerPostEncryptionPacketListener listener) {
        listener.onPing(this);
    }
}