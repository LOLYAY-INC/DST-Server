package io.lolyay.discordmsend.network.protocol.packet.packets.S2C.preenc;

import io.lolyay.discordmsend.network.protocol.listeners.client.ClientPreEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;
import io.lolyay.discordmsend.util.logging.Logger;

public record EncryptionRequestS2CPacket(
        byte[] publicKey,
        byte[] nonce
) implements Packet<ClientPreEncryptionPacketListener> {

    public static final PacketCodec<EncryptionRequestS2CPacket> CODEC = PacketCodec.create(
            (buf, packet) -> {
                buf.writeBytes(packet.publicKey());
                buf.writeBytes(packet.nonce());
            },
            (buf) -> new EncryptionRequestS2CPacket(
                    buf.IreadBytes(128),
                    buf.IreadBytes(16)
            )
    );

    @Override
    public void apply(ClientPreEncryptionPacketListener listener) {
        listener.onEncryptionRequest(this);
    }
}