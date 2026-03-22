package io.lolyay.discordmsend.network.protocol.coder;


import io.lolyay.discordmsend.network.protocol.Connection;
import io.lolyay.discordmsend.network.protocol.codec.PacketByteBuf;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;
import io.lolyay.discordmsend.network.protocol.packet.PacketDirection;
import io.lolyay.discordmsend.network.protocol.packet.PacketRegistry;
import io.lolyay.discordmsend.util.logging.BufDumper;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
@Slf4j
public class PacketDecoder extends MessageToMessageDecoder<ByteBuf> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PacketDecoder.class);

    private final PacketRegistry registry;
    private final Connection connection;
    private final PacketDirection direction;
    private final List<Integer > unknownPackets = new java.util.ArrayList<>();

    public PacketDecoder(PacketRegistry registry, Connection connection, PacketDirection direction) {
        this.registry = registry;
        this.connection = connection;
        this.direction = direction;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (!in.isReadable()) {
            return;
        }

        PacketByteBuf buf = new PacketByteBuf(in);
        int packetId = buf.readVarInt();
        PacketCodec<?> codec = registry.getCodec(connection.getPhase(), this.direction, packetId);
        if (codec == null) {
            if (!unknownPackets.contains(packetId)) {
                unknownPackets.add(packetId);
                log.warn("Unknown packet id: 0x{} for {}/{}", Integer.toHexString(packetId), connection.getPhase(), direction);
            }
            return;
        }

        Packet<?> packet = codec.decoder().apply(buf);
        

        if (in.readableBytes() > 0) {
            log.warn("Packet {} (0x{}) did not read all of its bytes! {} remaining.", packet.getClass().getSimpleName(), Integer.toHexString(packetId), in.readableBytes());
        }

        out.add(packet);
    }
}
