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
import io.netty.handler.codec.MessageToByteEncoder;

public class PacketEncoder extends MessageToByteEncoder<Packet<?>> {

    private final PacketRegistry registry;
    private final Connection connection;
    private final PacketDirection direction; // New field

    public PacketEncoder(PacketRegistry registry, Connection connection, PacketDirection direction) {
        this.registry = registry;
        this.connection = connection;
        this.direction = direction;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet<?> packet, ByteBuf out) throws Exception {
        try {
            int packetId = registry.getPacketId(connection.getPhase(), this.direction, packet);
            PacketByteBuf buf = new PacketByteBuf(out);

            buf.writeVarInt(packetId);

            // This is safe because the registry guarantees the codec matches the packet type.
            @SuppressWarnings("unchecked")
            var codec = (PacketCodec<Packet<?>>) registry.getCodec(connection.getPhase(), this.direction, packetId);

            codec.encoder().accept(buf, packet);

        } catch (Exception e){
            Logger.err("Failed to encode packet: " + packet.getClass().getSimpleName(), e);
            e.printStackTrace();
            throw e;
        }
    }
}