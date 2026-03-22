package io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc;


import io.lolyay.discordmsend.network.protocol.listeners.server.ServerPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;
import io.lolyay.discordmsend.network.types.ClientFeatures;

public record EncHelloC2SPacket(
        String userAgent,
        String userVersion,
        String userAuthor,
        ClientFeatures features,
        long botId
) implements Packet<ServerPostEncryptionPacketListener> {

    public static final PacketCodec<EncHelloC2SPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeString(packet.userAgent);
                buf.writeString(packet.userVersion);
                buf.writeString(packet.userAuthor);
                buf.writeByte(packet.features.toByte());
                buf.writeLong(packet.botId);
            },
            // Decoder
            (buf) -> new EncHelloC2SPacket(
                    buf.readString(),
                    buf.readString(),
                    buf.readString(),
                    new ClientFeatures(buf.readByte()),
                    buf.readLong()
            )
    );

    @Override
    public void apply(ServerPostEncryptionPacketListener listener) {
        listener.onEncHello(this);
    }
}