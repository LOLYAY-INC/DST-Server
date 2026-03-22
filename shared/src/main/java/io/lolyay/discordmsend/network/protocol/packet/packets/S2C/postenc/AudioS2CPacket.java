package io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc;


import io.lolyay.discordmsend.network.protocol.listeners.client.ClientPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;
import io.lolyay.discordmsend.obj.AudioCodec;

public record AudioS2CPacket(
        long guildId,
        AudioCodec codec,
        byte[] audioBytes,
        long sequence
) implements Packet<ClientPostEncryptionPacketListener> {
    /**
     * I hope the client likes music -
     */
    public static final PacketCodec<AudioS2CPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeLong(packet.guildId);
                buf.writeVarInt(packet.codec.ordinal());
                buf.writeVarInt(packet.audioBytes.length);
                buf.writeBytes(packet.audioBytes);
                buf.writeLong(packet.sequence);
            },
            // Decoder
            (buf) -> new AudioS2CPacket(
                    buf.readLong(),
                    AudioCodec.values()[buf.readVarInt()],
                    buf.readRawBytes(buf.readVarInt()),
                    buf.readLong()
            )
    );

    @Override
    public void apply(ClientPostEncryptionPacketListener listener) {
        listener.onAudio(this);
    }
}