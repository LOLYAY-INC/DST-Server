package io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc;


import io.lolyay.discordmsend.network.protocol.listeners.server.ServerPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;

public record SeekC2SPacket(
        long guildId,
        long positionMs

) implements Packet<ServerPostEncryptionPacketListener> {

    public static final PacketCodec<SeekC2SPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeLong(packet.guildId);
                buf.writeLong(packet.positionMs);
            },
            // Decoder
            (buf) -> new SeekC2SPacket(
                    buf.readLong(),
                    buf.readLong()
            )
    );

    @Override
    public void apply(ServerPostEncryptionPacketListener listener) {
        listener.onSeek(this);
    }
}