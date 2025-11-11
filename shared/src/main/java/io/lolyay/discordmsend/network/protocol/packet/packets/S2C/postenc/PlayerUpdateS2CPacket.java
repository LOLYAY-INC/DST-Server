package io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc;


import io.lolyay.discordmsend.network.protocol.listeners.client.ClientPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;

public record PlayerUpdateS2CPacket(
        long playerId,
        boolean paused,
        int volume,
        long position,
        boolean hasTrack,
        int trackId


) implements Packet<ClientPostEncryptionPacketListener> {
    /**
     * The codec for the entire LoginSuccessS2CPacket.
     */
    public static final PacketCodec<PlayerUpdateS2CPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeLong(packet.playerId());
                buf.writeBoolean(packet.paused());
                buf.writeInt(packet.volume());
                buf.writeLong(packet.position());
                buf.writeBoolean(packet.hasTrack());
                if (packet.hasTrack()) {
                    buf.writeInt(packet.trackId());
                }
            },
            // Decoder
            (buf) -> {
                long playerId = buf.readLong();
                boolean paused = buf.readBoolean();
                int volume = buf.readInt();
                long position = buf.readLong();
                boolean hasTrack = buf.readBoolean();
                int trackId = hasTrack ? buf.readInt() : -1;
                return new PlayerUpdateS2CPacket(playerId, paused, volume, position, hasTrack, trackId);
            }
    );

    @Override
    public void apply(ClientPostEncryptionPacketListener listener) {
        listener.onPlayerUpdate(this);
    }
}