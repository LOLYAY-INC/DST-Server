package io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc;


import io.lolyay.discordmsend.network.protocol.codec.PacketByteBuf;
import io.lolyay.discordmsend.network.protocol.listeners.client.ClientPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;
import it.unimi.dsi.fastutil.ints.IntArrayList;


public record CacheExpireS2C(
        IntArrayList expiredTrackIds
) implements Packet<ClientPostEncryptionPacketListener> {
    
    public static final PacketCodec<CacheExpireS2C> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> buf.writePrefixedArray(packet.expiredTrackIds(), PacketByteBuf::writeVarInt),
            // Decoder
            (buf) -> new CacheExpireS2C(new IntArrayList(buf.readPrefixedArray(PacketByteBuf::readVarInt)))
    );

    @Override
    public void apply(ClientPostEncryptionPacketListener listener) {
        listener.onCacheExpire(this);
    }
}
