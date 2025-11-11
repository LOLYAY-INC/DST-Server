package io.lolyay.discordmsend.network.protocol.packet;


import io.lolyay.discordmsend.network.protocol.NetworkPhase;
import io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc.*;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.*;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.ev.*;
import io.lolyay.discordmsend.network.protocol.packet.packets.C2S.preenc.EncryptionResponseC2SPacket;
import io.lolyay.discordmsend.network.protocol.packet.packets.C2S.preenc.HandShakeC2SPacket;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.preenc.EncryptionRequestS2CPacket;
import io.lolyay.discordmsend.util.logging.Logger;

import io.netty.util.collection.IntObjectHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.EnumMap;
import java.util.Map;

public class PacketRegistry {

    // The top-level map is still keyed by phase.
    private final Map<NetworkPhase, PhaseData> phaseData = new EnumMap<>(NetworkPhase.class);

    /**
     * This inner class now holds the maps for packet IDs and codecs.
     */
    private static class DirectionalData {
        final Map<Class<? extends Packet<?>>, Integer> packetToId = new Object2ObjectOpenHashMap<>();
        final Map<Integer, PacketCodec<?>> idToCodec = new IntObjectHashMap<>();
    }

    /**
     * A PhaseData now contains two DirectionalData instances: one for each direction.
     */
    private static class PhaseData {
        final Map<PacketDirection, DirectionalData> directionalData = new EnumMap<>(PacketDirection.class);
    }

    /**
     * Registers a packet with an explicit ID and DIRECTION.
     */
    public <T extends Packet<?>> void register(NetworkPhase phase, PacketDirection direction, int id, Class<T> packetClass, PacketCodec<T> codec) {
        // Get the data for the specific phase and direction, creating if it doesn't exist.
        DirectionalData data = phaseData
                .computeIfAbsent(phase, k -> new PhaseData())
                .directionalData
                .computeIfAbsent(direction, k -> new DirectionalData());

        if (data.idToCodec.containsKey(id)) {
            throw new IllegalArgumentException("Packet ID 0x" + Integer.toHexString(id) + " is already registered for " + phase + "/" + direction);
        }
        if (data.packetToId.containsKey(packetClass)) {
            throw new IllegalArgumentException("Packet " + packetClass.getSimpleName() + " is already registered for " + phase + "/" + direction);
        }

        data.idToCodec.put(id, codec);
        data.packetToId.put(packetClass, id);
    }

    // getPacketId and getCodec now need to know the direction to look in.

    public int getPacketId(NetworkPhase phase, PacketDirection direction, Packet<?> packet) {
        Integer id = phaseData.get(phase).directionalData.get(direction).packetToId.get(packet.getClass());
        if (id == null) {
            throw new IllegalArgumentException("Unregistered packet: " + packet.getClass().getSimpleName() + " for " + phase + "/" + direction);
        }
        return id;
    }

    public PacketCodec<?> getCodec(NetworkPhase phase, PacketDirection direction, int id) {
        PacketCodec<?> codec = phaseData.get(phase).directionalData.get(direction).idToCodec.get(id);
        if (codec == null) {
            return null;
           // throw new IllegalArgumentException("Unregistered packet id: 0x" + Integer.toHexString(id) + " for " + phase + "/" + direction);
        }
        return codec;
    }

    /**
     * Your corrected registration method. This now works without collision.
     */
    public void registerAll() {
        Logger.debug("Registering all protocol packets...");
        // PRE ENCRYPTION (Client to Server)
        register(NetworkPhase.PRE_ENCRYPTION, PacketDirection.SERVER_BOUND, 0, HandShakeC2SPacket.class, HandShakeC2SPacket.CODEC);
        register(NetworkPhase.PRE_ENCRYPTION, PacketDirection.SERVER_BOUND, 1, EncryptionResponseC2SPacket.class, EncryptionResponseC2SPacket.CODEC);

        // PRE ENCRYPTION (Server to Client)
        register(NetworkPhase.PRE_ENCRYPTION, PacketDirection.CLIENT_BOUND, 0, EncryptionRequestS2CPacket.class, EncryptionRequestS2CPacket.CODEC);  // 0x25 is 37



        // POST ENCRYPTION (Client to Server)
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.SERVER_BOUND, 0, EncHelloC2SPacket.class, EncHelloC2SPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.SERVER_BOUND, 1, KeepAliveC2SPacket.class, KeepAliveC2SPacket.CODEC);

        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.SERVER_BOUND, 2, DiscordDetailsC2SPacket.class, DiscordDetailsC2SPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.SERVER_BOUND, 3, PlayerCreateC2SPacket.class, PlayerCreateC2SPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.SERVER_BOUND, 4, PlayerConnectC2SPacket.class, PlayerConnectC2SPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.SERVER_BOUND, 5, SearchC2SPacket.class, SearchC2SPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.SERVER_BOUND, 6, RequestTrackInfoC2SPacket.class, RequestTrackInfoC2SPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.SERVER_BOUND, 7, PlayTrackC2SPacket.class, PlayTrackC2SPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.SERVER_BOUND, 8, SetDefaultVolumeC2SPacket.class, SetDefaultVolumeC2SPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.SERVER_BOUND, 9, PlayerPauseC2SPacket.class, PlayerPauseC2SPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.SERVER_BOUND, 10, PlayerResumeC2SPacket.class, PlayerResumeC2SPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.SERVER_BOUND, 11, PlayerSetVolumeC2SPacket.class, PlayerSetVolumeC2SPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.SERVER_BOUND, 12, PlayerStopC2SPacket.class, PlayerStopC2SPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.SERVER_BOUND, 13, PingC2SPacket.class, PingC2SPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.SERVER_BOUND, 14, SearchMultipleC2SPacket.class, SearchMultipleC2SPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.SERVER_BOUND, 15, SetSourceC2SPacket.class, SetSourceC2SPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.SERVER_BOUND, 16, InjectTrackC2SPacket.class, InjectTrackC2SPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.SERVER_BOUND, 17, RequestLinkC2SPacket.class, RequestLinkC2SPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.SERVER_BOUND, 18, ForceReconnectC2SPacket.class, ForceReconnectC2SPacket.CODEC);

        // POST ENCRYPTION (Server to Client)
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.CLIENT_BOUND, 0, EncHelloS2CPacket.class, EncHelloS2CPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.CLIENT_BOUND, 1, KeepAliveS2CPacket.class, KeepAliveS2CPacket.CODEC);

        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.CLIENT_BOUND, 2, TrackDetailsS2CPacket.class, TrackDetailsS2CPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.CLIENT_BOUND, 3, TrackSearchResponseS2CPacket.class, TrackSearchResponseS2CPacket.CODEC);

        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.CLIENT_BOUND, 4, PlayerPauseS2CPacket.class,      PlayerPauseS2CPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.CLIENT_BOUND, 5, PlayerResumeS2CPacket.class,     PlayerResumeS2CPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.CLIENT_BOUND, 6, PlayerTrackEndS2CPacket.class,   PlayerTrackEndS2CPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.CLIENT_BOUND, 7, PlayerTrackFailS2CPacket.class,  PlayerTrackFailS2CPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.CLIENT_BOUND, 8, PlayerTrackStartS2CPacket.class, PlayerTrackStartS2CPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.CLIENT_BOUND, 9, PlayerTrackStuckS2CPacket.class, PlayerTrackStuckS2CPacket.CODEC);

        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.CLIENT_BOUND, 10, PongS2CPacket.class, PongS2CPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.CLIENT_BOUND, 11, PlayerUpdateS2CPacket.class, PlayerUpdateS2CPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.CLIENT_BOUND, 12, StatisticsS2CPacket.class, StatisticsS2CPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.CLIENT_BOUND, 13, SearchMultipleResponseS2CPacket.class, SearchMultipleResponseS2CPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.CLIENT_BOUND, 14, CacheExpireS2C.class, CacheExpireS2C.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.CLIENT_BOUND, 15, LinkResponseS2CPacket.class, LinkResponseS2CPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.CLIENT_BOUND, 16, TrackTimingUpdateS2CPacket.class, TrackTimingUpdateS2CPacket.CODEC);
        register(NetworkPhase.POST_ENCRYPTION, PacketDirection.CLIENT_BOUND, 17, AudioS2CPacket.class, AudioS2CPacket.CODEC);


    }
}