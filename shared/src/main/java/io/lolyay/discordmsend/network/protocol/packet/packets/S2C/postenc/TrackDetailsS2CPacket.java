package io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc;


import io.lolyay.discordmsend.network.protocol.listeners.client.ClientPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;

public record TrackDetailsS2CPacket(
        int id,
        String titleName,
        String author,
        String art,
        long duration


) implements Packet<ClientPostEncryptionPacketListener> {
    /**
     * The codec for the entire LoginSuccessS2CPacket.
     */
    public static final PacketCodec<TrackDetailsS2CPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeVarInt(packet.id());
                buf.writeString(packet.titleName() == null ? "UNKNOWN" : packet.titleName());
                buf.writeString(packet.author() == null ? "UNKNOWN" : packet.author());
                buf.writeString(packet.art() == null ? "UNKNOWN" : packet.art());
                buf.writeLong(packet.duration());
            },
            // Decoder
            (buf) -> {
                return new TrackDetailsS2CPacket(
                        buf.readVarInt(),
                        buf.readString(),
                        buf.readString(),
                        buf.readString(),
                        buf.readLong()
                );
            }
    );

    @Override
    public void apply(ClientPostEncryptionPacketListener listener) {
        listener.onTrackDetails(this);
    }
}