package io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.ev;


import io.lolyay.discordmsend.network.protocol.listeners.client.ClientPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;

public record PlayerTrackStuckS2CPacket(
        long guildId,
        int tackId,
        long timeoutMs
) implements Packet<ClientPostEncryptionPacketListener> {
    /**
     * The codec for the entire LoginSuccessS2CPacket.
     */
    public static final PacketCodec<PlayerTrackStuckS2CPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeLong(packet.guildId());
                buf.writeVarInt(packet.tackId());
                buf.writeLong(packet.timeoutMs());
            },
            // Decoder
            (buf) -> {
                return new PlayerTrackStuckS2CPacket(buf.readLong(), buf.readVarInt(), buf.readLong());
            }
    );

    @Override
    public void apply(ClientPostEncryptionPacketListener listener) {
        listener.onPlayerTrackStuck(this);
    }
}