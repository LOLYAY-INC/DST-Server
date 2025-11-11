package io.lolyay.discordmsend.network.protocol.packet;



import io.lolyay.discordmsend.network.protocol.codec.PacketByteBuf;

import java.util.function.BiConsumer;
import java.util.function.Function;

public record PacketCodec<T extends Packet<?>>(
        BiConsumer<PacketByteBuf, T> encoder,
        Function<PacketByteBuf, T> decoder
) {
    public static <T extends Packet<?>> PacketCodec<T> create(BiConsumer<PacketByteBuf, T> encoder, Function<PacketByteBuf, T> decoder) {
        return new PacketCodec<>(encoder, decoder);
    }
}