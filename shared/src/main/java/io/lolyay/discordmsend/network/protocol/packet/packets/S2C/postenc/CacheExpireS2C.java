package io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc;


import io.lolyay.discordmsend.network.protocol.listeners.client.ClientPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;

import java.util.List;

/**
 * Notifies the client that certain track IDs have been expired from the server cache.
 * The client should remove these entries from its local cache to stay synchronized.
 */
public record CacheExpireS2C(
        List<Integer> expiredTrackIds
) implements Packet<ClientPostEncryptionPacketListener> {
    
    public static final PacketCodec<CacheExpireS2C> CODEC = PacketCodec.create(
            // Encoder
            (buf, packet) -> {
                buf.writeVarInt(packet.expiredTrackIds().size());
                for (int trackId : packet.expiredTrackIds()) {
                    buf.writeVarInt(trackId);
                }
            },
            // Decoder
            (buf) -> {
                int size = buf.readVarInt();
                List<Integer> expiredIds = new java.util.ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    expiredIds.add(buf.readVarInt());
                }
                return new CacheExpireS2C(expiredIds);
            }
    );

    @Override
    public void apply(ClientPostEncryptionPacketListener listener) {
        listener.onCacheExpire(this);
    }
}
