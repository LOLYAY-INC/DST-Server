package io.lolyay.discordmsend.network.protocol.coder;


import io.lolyay.discordmsend.network.protocol.Connection;
import io.lolyay.discordmsend.network.protocol.codec.PacketByteBuf;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketCodec;
import io.lolyay.discordmsend.network.protocol.packet.PacketDirection;
import io.lolyay.discordmsend.network.protocol.packet.PacketRegistry;
import io.lolyay.discordmsend.util.logging.BufDumper;
import io.lolyay.discordmsend.util.logging.Logger;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.List;

/**
 * Decodes a single packet frame (a ByteBuf) into a Packet object.
 * This handler MUST be placed after a frame decoder (like ProtobufVarint32FrameDecoder)
 * in the pipeline.
 */
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

    /**
     * This method is now called by Netty once for each complete frame (packet)
     * emitted by the preceding VarintFrameDecoder.
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (!in.isReadable()) {
            return;
        }

        // =================================================================
        //  NEW HEXDUMP CODE
        //  This prints the raw bytes of the framed and decrypted packet.
        // =================================================================
        //ogger.debug("--- INCOMING PACKET FRAME (" + in.readableBytes() + " bytes) for phase " + connection.getPhase() + " ---");
        // The hexdump does not consume the bytes, it only reads them for printing.
       // System.out.println(ByteBufUtil.prettyHexDump(in));
        //logger.debug("--------------------------------------------------");
        // =================================================================

        PacketByteBuf buf = new PacketByteBuf(in);
        int packetId = buf.readVarInt();
        PacketCodec<?> codec = registry.getCodec(connection.getPhase(), this.direction, packetId);
        if (codec == null) {
            if (!unknownPackets.contains(packetId)) {
                unknownPackets.add(packetId);
                Logger.warn("Unknown packet id: 0x" + Integer.toHexString(packetId) + " for " + connection.getPhase() + "/" + direction);
            }
            return;
        }

        Packet<?> packet = codec.decoder().apply(buf);
        
        // Debug logging for SetSourceC2SPacket

        if (in.readableBytes() > 0) {
            Logger.warn("Packet " + packet.getClass().getSimpleName() + " (0x" + Integer.toHexString(packetId) + ") did not read all of its bytes! " + in.readableBytes() + " remaining.");
        }

        out.add(packet);
    }
}
