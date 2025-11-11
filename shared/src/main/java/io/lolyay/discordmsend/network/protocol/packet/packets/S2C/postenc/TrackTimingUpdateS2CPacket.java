package io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc;


import io.lolyay.discordmsend.network.protocol.listeners.client.ClientPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;

/**
 * Notifies the client that certain track IDs have been expired from the server cache.
 * The client should remove these entries from its local cache to stay synchronized.
 */
public record TrackTimingUpdateS2CPacket(
        TrackTimingType timingType,
        long guildId,
        long timestamp

) implements Packet<ClientPostEncryptionPacketListener> {
    
    public static final PacketCodec<TrackTimingUpdateS2CPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeVarInt(packet.timingType().ordinal());
                buf.writeLong(packet.guildId());
                buf.writeLong(packet.timestamp());
            },
            // Decoder
            (buf) -> {
                return new TrackTimingUpdateS2CPacket(
                        TrackTimingType.values()[buf.readVarInt()],
                        buf.readLong(),
                        buf.readLong()
                );
            }
    );

    public static enum TrackTimingType{
        STARTED,
        PAUSED,
        RESUMED,
        STOPPED,
        ADJUST
    }

    @Override
    public void apply(ClientPostEncryptionPacketListener listener) {
        listener.onTrackTimingUpdate(this);
    }
}
