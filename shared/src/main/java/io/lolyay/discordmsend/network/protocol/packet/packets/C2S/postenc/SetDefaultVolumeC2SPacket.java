package io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc;


import io.lolyay.discordmsend.network.protocol.listeners.server.ServerPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;

public record SetDefaultVolumeC2SPacket(
        int volume
) implements Packet<ServerPostEncryptionPacketListener> {
    /**
     * The codec for the entire LoginSuccessS2CPacket.
     */
    public static final PacketCodec<SetDefaultVolumeC2SPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeVarInt(packet.volume);
            },
            // Decoder
            (buf) -> {
                return new SetDefaultVolumeC2SPacket(
                        buf.readVarInt()
                );
            }
    );

    @Override
    public void apply(ServerPostEncryptionPacketListener listener) {
        listener.onDefaultVolume(this);
    }
}