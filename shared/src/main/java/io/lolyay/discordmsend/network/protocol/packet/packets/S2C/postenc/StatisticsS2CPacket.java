package io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc;


import io.lolyay.discordmsend.network.protocol.listeners.client.ClientPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;

public record StatisticsS2CPacket(
        long maxMem,
        long freeMem,
        int cpuUsageP,
        int players,
        int clients
) implements Packet<ClientPostEncryptionPacketListener> {
    /**
     * The codec for the entire LoginSuccessS2CPacket.
     */
    public static final PacketCodec<StatisticsS2CPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeLong(packet.maxMem());
                buf.writeLong(packet.freeMem());
                buf.writeInt(packet.cpuUsageP());
                buf.writeVarInt(packet.players());
                buf.writeVarInt(packet.clients());
            },
            // Decoder
            (buf) -> {
                return new StatisticsS2CPacket(buf.readLong(), buf.readLong(), buf.readInt(), buf.readVarInt(), buf.readVarInt());
            }
    );

    @Override
    public void apply(ClientPostEncryptionPacketListener listener) {
        listener.onStatistics(this);
    }
}