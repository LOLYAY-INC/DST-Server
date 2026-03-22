package io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc;

import io.lolyay.discordmsend.network.protocol.codec.PacketByteBuf;
import io.lolyay.discordmsend.network.protocol.listeners.client.ClientPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;
import io.lolyay.discordmsend.network.protocol.request.IResponsePacket;

import java.util.Optional;

import static io.lolyay.discordmsend.network.protocol.request.LinkRequest.EXCHANGE_TYPE;

public record LinkResponseS2CPacket(
        long guildId,
        int sequence,
        String link,
        boolean success,
        String errorMessage
) implements Packet<ClientPostEncryptionPacketListener>, IResponsePacket {
    
    public static final PacketCodec<LinkResponseS2CPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeLong(packet.guildId);
                buf.writeVarInt(packet.sequence);
                buf.writeOptional(Optional.ofNullable(packet.link), PacketByteBuf::writeString);
                buf.writeBoolean(packet.success);
                buf.writeOptional(Optional.ofNullable(packet.errorMessage), PacketByteBuf::writeString);
            },
            // Decoder
            (buf) -> new LinkResponseS2CPacket(
                    buf.readLong(),
                    buf.readVarInt(),
                    buf.readOptional(PacketByteBuf::readString).orElse(null),
                    buf.readBoolean(),
                    buf.readOptional(PacketByteBuf::readString).orElse(null)
            )
    );
    
    public static LinkResponseS2CPacket success(long guildId, int sequence, String link) {
        return new LinkResponseS2CPacket(guildId, sequence, link, true, null);
    }
    
    public static LinkResponseS2CPacket error(long guildId, int sequence, String errorMessage) {
        return new LinkResponseS2CPacket(guildId, sequence, null, false, errorMessage);
    }

    @Override
    public int getExchangeType() {
        return EXCHANGE_TYPE;
    }

    @Override
    public void apply(ClientPostEncryptionPacketListener listener) {
        listener.onResponse(this);
    }
}
