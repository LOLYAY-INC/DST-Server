package io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.ev;


import io.lolyay.discordmsend.network.protocol.listeners.client.ClientPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;
import io.lolyay.discordmsend.obj.Severity;

public record PlayerTrackFailS2CPacket(
        long guildId,
        int tackId,
        Severity severity,
        String message
) implements Packet<ClientPostEncryptionPacketListener> {
    /**
     * The codec for the entire LoginSuccessS2CPacket.
     */
    public static final PacketCodec<PlayerTrackFailS2CPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeLong(packet.guildId());
                buf.writeVarInt(packet.tackId());
                buf.writeVarInt(packet.severity().ordinal());
                buf.writeString(packet.message());
            },
            // Decoder
            (buf) -> {
                return new PlayerTrackFailS2CPacket(buf.readLong(), buf.readVarInt(), Severity.values()[buf.readVarInt()], buf.readString());
            }
    );

    @Override
    public void apply(ClientPostEncryptionPacketListener listener) {
        listener.onPlayerTrackError(this);
    }
}