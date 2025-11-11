package io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc;


import io.lolyay.discordmsend.network.protocol.listeners.server.ServerPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;

public record PlayTrackC2SPacket(
        int trackId,
        long guildId
) implements Packet<ServerPostEncryptionPacketListener> {
    /**
     * The codec for the entire LoginSuccessS2CPacket.
     */
    public static final PacketCodec<PlayTrackC2SPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeVarInt(packet.trackId);
                buf.writeLong(packet.guildId);
            },
            // Decoder
            (buf) -> {
                return new PlayTrackC2SPacket(
                        buf.readVarInt(),
                        buf.readLong()
                );
            }
    );

    @Override
    public void apply(ServerPostEncryptionPacketListener listener) {
        listener.onPlayTrack(this);
    }
}