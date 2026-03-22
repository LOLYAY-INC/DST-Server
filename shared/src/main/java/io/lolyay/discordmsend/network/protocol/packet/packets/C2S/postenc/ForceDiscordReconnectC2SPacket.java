package io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc;


import io.lolyay.discordmsend.network.protocol.listeners.server.ServerPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;

public record ForceDiscordReconnectC2SPacket(
    long guildId
) implements Packet<ServerPostEncryptionPacketListener> {

    public static final PacketCodec<ForceDiscordReconnectC2SPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeLong(packet.guildId);
            },
            // Decoder
            (buf) -> new ForceDiscordReconnectC2SPacket(
                    buf.readLong()
            )
    );

    @Override
    public void apply(ServerPostEncryptionPacketListener listener) {
        listener.onForceReconnect(this);
    }
}