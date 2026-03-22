package io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc;

import io.lolyay.discordmsend.network.protocol.listeners.server.ServerPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;
import io.lolyay.discordmsend.network.protocol.request.IRequestPacket;

import static io.lolyay.discordmsend.network.protocol.request.LinkRequest.EXCHANGE_TYPE;

public record RequestLinkC2SPacket(
        long guildId,
        int sequence
) implements Packet<ServerPostEncryptionPacketListener>, IRequestPacket {
    public static final PacketCodec<RequestLinkC2SPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeLong(packet.guildId);
                buf.writeVarInt(packet.sequence);
            },
            // Decoder
            (buf) -> new RequestLinkC2SPacket(
                    buf.readLong(),
                    buf.readVarInt()
            )
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
