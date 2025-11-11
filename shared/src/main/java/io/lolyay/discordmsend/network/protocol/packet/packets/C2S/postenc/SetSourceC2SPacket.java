package io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc;


import io.lolyay.discordmsend.network.protocol.listeners.server.ServerPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;

public record SetSourceC2SPacket(
        long guildId,
        boolean ytdlp
) implements Packet<ServerPostEncryptionPacketListener> {
    /**
     * The codec for the entire LoginSuccessS2CPacket.
     */
    public static final PacketCodec<SetSourceC2SPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeLong(packet.guildId);
                buf.writeBoolean(packet.ytdlp);

            },
            // Decoder
            (buf) -> {
                long guildId = buf.readLong();
                boolean ytdlp = buf.readBoolean();
                return new SetSourceC2SPacket(guildId, ytdlp);
            }
    );

    @Override
    public void apply(ServerPostEncryptionPacketListener listener) {
        listener.onChangeSource(this);
    }
}