package io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc;


import io.lolyay.discordmsend.network.protocol.listeners.server.ServerPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;

public record InjectTrackC2SPacket(
        String trackUri,
        int sequence,
        boolean details,
        boolean skipSearch
) implements Packet<ServerPostEncryptionPacketListener> {
    /**
     * The codec for the entire LoginSuccessS2CPacket.
     */
    public static final PacketCodec<InjectTrackC2SPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeString(packet.trackUri());
                buf.writeVarInt(packet.sequence);
                buf.writeBoolean(packet.details());
                buf.writeBoolean(packet.skipSearch);
            },
            // Decoder
            (buf) -> {
                return new InjectTrackC2SPacket(buf.readString(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean());
            }
    );

    @Override
    public void apply(ServerPostEncryptionPacketListener listener) {
        listener.onTrackInject(this);
    }
}