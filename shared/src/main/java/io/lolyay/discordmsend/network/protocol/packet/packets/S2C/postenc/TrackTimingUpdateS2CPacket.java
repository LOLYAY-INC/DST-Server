package io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc;


import io.lolyay.discordmsend.network.protocol.listeners.client.ClientPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;


public record TrackTimingUpdateS2CPacket(
        TrackTimingUpdateType timingType,
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
            (buf) -> new TrackTimingUpdateS2CPacket(
                    TrackTimingUpdateType.values()[buf.readVarInt()],
                    buf.readLong(),
                    buf.readLong()
            )
    );

    public enum TrackTimingUpdateType {
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
