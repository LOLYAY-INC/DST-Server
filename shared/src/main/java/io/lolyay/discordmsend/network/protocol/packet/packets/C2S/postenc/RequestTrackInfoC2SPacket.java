package io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc;

import io.lolyay.discordmsend.network.protocol.listeners.server.ServerPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;
import io.lolyay.discordmsend.network.protocol.request.IRequestPacket;

import static io.lolyay.discordmsend.network.protocol.request.TrackInfoRequest.EXCHANGE_TYPE;

public record RequestTrackInfoC2SPacket(
        int trackId
) implements Packet<ServerPostEncryptionPacketListener>, IRequestPacket {

    public static final PacketCodec<RequestTrackInfoC2SPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeVarInt(packet.trackId);
            },
            // Decoder
            (buf) -> new RequestTrackInfoC2SPacket(
                    buf.readVarInt()
            )
    );

    @Override
    public void apply(ServerPostEncryptionPacketListener listener) {
        listener.onRequest(this);
    }

    @Override
    public int sequence() {
        return trackId;
    }

    @Override
    public int getExchangeType() {
        return EXCHANGE_TYPE;
    }
}