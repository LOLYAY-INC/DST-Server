package io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc;


import io.lolyay.discordmsend.network.protocol.listeners.client.ClientPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;
import io.lolyay.discordmsend.network.types.ModdedInfo;
import io.lolyay.discordmsend.network.types.ServerFeatures;


public record EncHelloS2CPacket(
        String serverName,
        String serverVersion,
        int serverProtocolId,
        ServerFeatures features,
        String ytSourceVersion,
        ModdedInfo moddedInfo,
        String countryCode


) implements Packet<ClientPostEncryptionPacketListener> {

    public static final PacketCodec<EncHelloS2CPacket> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeString(packet.serverName());
                buf.writeString(packet.serverVersion());
                buf.writeShort(packet.serverProtocolId());
                buf.writeByte(packet.features().toByte());
                buf.writeString(packet.ytSourceVersion());
                packet.moddedInfo().write(buf);
                buf.writeString(packet.countryCode());
            },
            // Decoder
            (buf) -> new EncHelloS2CPacket(
                    buf.readString(),
                    buf.readString(),
                    buf.readShort(),
                    new ServerFeatures(buf.readByte()),
                    buf.readString(),
                    new ModdedInfo(buf),
                    buf.readString()
            )
    );

    @Override
    public void apply(ClientPostEncryptionPacketListener listener) {
        listener.onEncHello(this);
    }
}