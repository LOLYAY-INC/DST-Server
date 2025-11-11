package io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc;

import io.lolyay.discordmsend.network.protocol.listeners.server.ServerPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;

public record RequestLinkC2SPacket(
        long guildId,
        int sequence
) implements Packet<ServerPostEncryptionPacketListener> {
    public static final PacketCodec<RequestLinkC2SPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeLong(packet.guildId);
                buf.writeVarInt(packet.sequence);
            },
            // Decoder
            (buf) -> {
                return new RequestLinkC2SPacket(
                        buf.readLong(),
                        buf.readVarInt()
                );
            }
    );

    @Override
    public void apply(ServerPostEncryptionPacketListener listener) {
        listener.onRequestLink(this);
    }
}
