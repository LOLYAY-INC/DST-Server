package io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc;


import io.lolyay.discordmsend.network.protocol.listeners.server.ServerPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;

public record PlayerConnectC2SPacket(
        long guildId,
        String endPoint,
        String sessionId,
        String token

) implements Packet<ServerPostEncryptionPacketListener> {
    /**
     * The codec for the entire LoginSuccessS2CPacket.
     */
    public static final PacketCodec<PlayerConnectC2SPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeLong(packet.guildId);
                buf.writeString(packet.endPoint);
                buf.writeString(packet.sessionId);
                buf.writeString(packet.token);
            },
            // Decoder
            (buf) -> {
                return new PlayerConnectC2SPacket(
                        buf.readLong(),
                        buf.readString(),
                        buf.readString(),
                        buf.readString()
                );
            }
    );

    @Override
    public void apply(ServerPostEncryptionPacketListener listener) {
        listener.onPlayerConnect(this);
    }
}