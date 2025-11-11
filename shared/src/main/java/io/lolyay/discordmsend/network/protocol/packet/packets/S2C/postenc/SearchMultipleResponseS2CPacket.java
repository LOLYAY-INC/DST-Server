package io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc;


import io.lolyay.discordmsend.network.protocol.codec.PacketByteBuf;
import io.lolyay.discordmsend.network.protocol.listeners.client.ClientPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;

import java.util.List;

public record SearchMultipleResponseS2CPacket(
        List<Integer> ids,
        int sequence

) implements Packet<ClientPostEncryptionPacketListener> {
    /**
     * The codec for the entire LoginSuccessS2CPacket.
     */
    public static final PacketCodec<SearchMultipleResponseS2CPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writePrefixedArray(packet.ids, PacketByteBuf::writeVarInt);
                buf.writeVarInt(packet.sequence());

            },
            // Decoder
            (buf) -> {
                return new SearchMultipleResponseS2CPacket(
                        buf.readPrefixedArray(PacketByteBuf::readVarInt),
                        buf.readVarInt()
                );
            }
    );

    @Override
    public void apply(ClientPostEncryptionPacketListener listener) {
        listener.onSearchMultiple(this);
    }
}