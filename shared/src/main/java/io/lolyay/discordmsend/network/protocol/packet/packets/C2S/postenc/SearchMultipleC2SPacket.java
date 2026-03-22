package io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc;


import io.lolyay.discordmsend.network.protocol.listeners.server.ServerPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;
import io.lolyay.discordmsend.network.protocol.request.IRequestPacket;

import static io.lolyay.discordmsend.network.protocol.request.SearchRequest.EXCHANGE_TYPE;


public record SearchMultipleC2SPacket(
        String query,
        int sequence,
        boolean details,
        int maxResults
) implements Packet<ServerPostEncryptionPacketListener>, IRequestPacket {
    public static final PacketCodec<SearchMultipleC2SPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeString(packet.query());
                buf.writeVarInt(packet.sequence);
                buf.writeBoolean(packet.details);
                buf.writeVarInt(packet.maxResults);
            },
            // Decoder
            (buf) -> {
                return new SearchMultipleC2SPacket(
                        buf.readString(),
                        buf.readVarInt(),
                        buf.readBoolean(),
                        buf.readVarInt()
                );
            }
    );

    @Override
    public int getExchangeType() {
        return EXCHANGE_TYPE;
    }

    @Override
    public void apply(ServerPostEncryptionPacketListener listener) {
        listener.onRequest(this);
    }
}