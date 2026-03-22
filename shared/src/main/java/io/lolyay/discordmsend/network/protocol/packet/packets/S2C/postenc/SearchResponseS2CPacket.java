package io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc;


import io.lolyay.discordmsend.network.protocol.codec.PacketByteBuf;
import io.lolyay.discordmsend.network.protocol.listeners.client.ClientPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;
import io.lolyay.discordmsend.network.protocol.request.IResponsePacket;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import static io.lolyay.discordmsend.network.protocol.request.SearchRequest.EXCHANGE_TYPE;

public record SearchResponseS2CPacket(
        IntArrayList trackIds,
        int sequence
) implements Packet<ClientPostEncryptionPacketListener>, IResponsePacket {

    public static final PacketCodec<SearchResponseS2CPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writePrefixedArray(packet.trackIds, PacketByteBuf::writeVarInt);
                buf.writeVarInt(packet.sequence());

            },
            // Decoder
            (buf) -> new SearchResponseS2CPacket(
                    new IntArrayList(buf.readPrefixedArray(PacketByteBuf::readVarInt)),
                    buf.readVarInt()
            )
    );

    @Override
    public int getExchangeType() {
        return EXCHANGE_TYPE;
    }

    @Override
    public void apply(ClientPostEncryptionPacketListener listener) {
        listener.onResponse(this);
    }
}