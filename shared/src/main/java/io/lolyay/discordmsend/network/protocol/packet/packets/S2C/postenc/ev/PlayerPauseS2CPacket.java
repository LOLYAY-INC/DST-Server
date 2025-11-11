package io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.ev;


import io.lolyay.discordmsend.network.protocol.listeners.client.ClientPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;

public record PlayerPauseS2CPacket(
        long guildId
) implements Packet<ClientPostEncryptionPacketListener> {
    /**
     * The codec for the entire LoginSuccessS2CPacket.
     */
    public static final PacketCodec<PlayerPauseS2CPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeLong(packet.guildId());
            },
            // Decoder
            (buf) -> {
                return new PlayerPauseS2CPacket(buf.readLong());
            }
    );

    @Override
    public void apply(ClientPostEncryptionPacketListener listener) {
        listener.onPlayerPause(this);
    }
}