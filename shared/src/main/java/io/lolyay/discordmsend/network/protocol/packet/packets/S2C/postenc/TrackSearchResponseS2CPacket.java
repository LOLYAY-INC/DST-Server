package io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc;


import io.lolyay.discordmsend.network.protocol.listeners.client.ClientPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;

public record TrackSearchResponseS2CPacket(
        int id,
        int sequence

) implements Packet<ClientPostEncryptionPacketListener> {
    /**
     * The codec for the entire LoginSuccessS2CPacket.
     */
    public static final PacketCodec<TrackSearchResponseS2CPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeVarInt(packet.id());
                buf.writeVarInt(packet.sequence());

            },
            // Decoder
            (buf) -> {
                return new TrackSearchResponseS2CPacket(
                        buf.readVarInt(),
                        buf.readVarInt()
                );
            }
    );

    @Override
    public void apply(ClientPostEncryptionPacketListener listener) {
        listener.onSearchResponse(this);
    }
}