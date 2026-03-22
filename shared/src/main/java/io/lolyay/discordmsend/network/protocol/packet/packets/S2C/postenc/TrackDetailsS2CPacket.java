package io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc;

import io.lolyay.discordmsend.network.protocol.listeners.client.ClientPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;
import io.lolyay.discordmsend.network.protocol.request.IResponsePacket;
import io.lolyay.discordmsend.network.types.TrackMetadata;

import static io.lolyay.discordmsend.network.protocol.request.TrackInfoRequest.EXCHANGE_TYPE;

public record TrackDetailsS2CPacket(
        TrackMetadata metadata
) implements Packet<ClientPostEncryptionPacketListener>, IResponsePacket {
    public static final PacketCodec<TrackDetailsS2CPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeVarInt(packet.metadata.id());
                buf.writeString(packet.metadata.trackName() == null ? "UNKNOWN" : packet.metadata.trackName());
                buf.writeString(packet.metadata.author() == null ? "UNKNOWN" : packet.metadata.author());
                buf.writeString(packet.metadata.art() == null ? "UNKNOWN" : packet.metadata.art());
                buf.writeString(packet.metadata.trackUrl() == null ? "UNKNOWN" : packet.metadata.trackUrl());
                buf.writeString(packet.metadata.isrc() == null ? "UNKNOWN" : packet.metadata.isrc());
                buf.writeLong(packet.metadata.durationMs());
                buf.writeString(packet.metadata.sourceId() == null ? "UNKNOWN" : packet.metadata.sourceId());
                buf.writeString(packet.metadata.identifier() == null ? "UNKNOWN" : packet.metadata.identifier());
            },
            // Decoder
            (buf) -> {
                return new TrackDetailsS2CPacket(
                        new TrackMetadata(
                                buf.readVarInt(),
                                buf.readString(),
                                buf.readString(),
                                buf.readString(),
                                buf.readString(),
                                buf.readString(),
                                buf.readLong(),
                                buf.readString(),
                                buf.readString()
                        )
                );
            }
    );

    @Override
    public void apply(ClientPostEncryptionPacketListener listener) {
        listener.onTrackDetails(this);
    }

    @Override
    public int sequence() {
        return metadata.id();
    }

    @Override
    public int getExchangeType() {
        return EXCHANGE_TYPE;
    }
}