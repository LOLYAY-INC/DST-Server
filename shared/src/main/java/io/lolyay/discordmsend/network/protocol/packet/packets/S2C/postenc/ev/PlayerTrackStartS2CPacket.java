package io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.ev;


import io.lolyay.discordmsend.network.protocol.listeners.client.ClientPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;

public record PlayerTrackStartS2CPacket(
        long guildId,
        int trackId
) implements Packet<ClientPostEncryptionPacketListener> {
    /**
     * The codec for the entire LoginSuccessS2CPacket.
     */
    public static final PacketCodec<PlayerTrackStartS2CPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeLong(packet.guildId());
                buf.writeVarInt(packet.trackId());
            },
            // Decoder
            (buf) -> {
                return new PlayerTrackStartS2CPacket(buf.readLong(), buf.readVarInt());
            }
    );

    @Override
    public void apply(ClientPostEncryptionPacketListener listener) {
        listener.onPlayerTrackStart(this);
    }
}