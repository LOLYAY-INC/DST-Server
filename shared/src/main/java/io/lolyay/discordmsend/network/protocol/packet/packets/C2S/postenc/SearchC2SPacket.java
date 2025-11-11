package io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc;


import io.lolyay.discordmsend.network.protocol.listeners.server.ServerPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;

public record SearchC2SPacket(
        String query,
        boolean music,
        int sequence,
        boolean details
) implements Packet<ServerPostEncryptionPacketListener> {
    /**
     * The codec for the entire LoginSuccessS2CPacket.
     */
    public static final PacketCodec<SearchC2SPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeString(packet.query());
                buf.writeBoolean(packet.music);
                buf.writeVarInt(packet.sequence);
                buf.writeBoolean(packet.details);
            },
            // Decoder
            (buf) -> {
                return new SearchC2SPacket(
                        buf.readString(),
                        buf.readBoolean(),
                        buf.readVarInt(),
                        buf.readBoolean()
                );
            }
    );

    @Override
    public void apply(ServerPostEncryptionPacketListener listener) {
        listener.onSearch(this);
    }
}