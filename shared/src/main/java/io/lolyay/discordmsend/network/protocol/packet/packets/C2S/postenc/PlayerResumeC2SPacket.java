package io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc;


import io.lolyay.discordmsend.network.protocol.listeners.server.ServerPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;

public record PlayerResumeC2SPacket(
        long guildId

) implements Packet<ServerPostEncryptionPacketListener> {
    /**
     * The codec for the entire LoginSuccessS2CPacket.
     */
    public static final PacketCodec<PlayerResumeC2SPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeLong(packet.guildId);
            },
            // Decoder
            (buf) -> {
                return new PlayerResumeC2SPacket(
                        buf.readLong()
                );
            }
    );

    @Override
    public void apply(ServerPostEncryptionPacketListener listener) {
        listener.onPlayerResume(this);
    }
}