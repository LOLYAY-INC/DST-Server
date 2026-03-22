package io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc;


import io.lolyay.discordmsend.network.protocol.listeners.server.ServerPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;

public record PlayerDiscordConnectC2SPacket(
        long guildId,
        String endPoint,
        String sessionId,
        String token,
        long channelId

) implements Packet<ServerPostEncryptionPacketListener> {

    public static final PacketCodec<PlayerDiscordConnectC2SPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeLong(packet.guildId);
                buf.writeString(packet.endPoint);
                buf.writeString(packet.sessionId);
                buf.writeString(packet.token);
                buf.writeLong(packet.guildId);
            },
            // Decoder
            (buf) -> new PlayerDiscordConnectC2SPacket(
                    buf.readLong(),
                    buf.readString(),
                    buf.readString(),
                    buf.readString(),
                    buf.readLong()
            )
    );

    @Override
    public void apply(ServerPostEncryptionPacketListener listener) {
        listener.onDiscordDetails(this);
    }
}