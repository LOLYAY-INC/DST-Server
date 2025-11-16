package io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc;


import io.lolyay.discordmsend.network.protocol.listeners.client.ClientPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;

public record AudioS2CPacket(
        long guildId,
        byte[] opus,
        long sequence
) implements Packet<ClientPostEncryptionPacketListener> {
    /**
     * I hope the client likes music -
     */
    public static final PacketCodec<AudioS2CPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeLong(packet.guildId);
                buf.writeVarInt(packet.opus.length);
                buf.writeBytes(packet.opus);
                buf.writeLong(packet.sequence);
            },
            // Decoder
            (buf) -> {
                return new AudioS2CPacket(buf.readLong(), buf.IreadBytes(buf.readVarInt()), buf.readLong());
            }
    );

    @Override
    public void apply(ClientPostEncryptionPacketListener listener) {
        listener.onAudio(this);
    }
}