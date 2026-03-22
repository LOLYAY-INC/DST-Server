package io.lolyay.discordmsend.server.network;
import io.lolyay.discordmsend.network.Enviroment;
import io.lolyay.discordmsend.network.protocol.Connection;
import io.lolyay.discordmsend.network.protocol.NetworkPhase;
import io.lolyay.discordmsend.network.protocol.coder.PacketDecoder;
import io.lolyay.discordmsend.network.protocol.coder.PacketEncoder;
import io.lolyay.discordmsend.network.protocol.packet.PacketDirection;
import io.lolyay.discordmsend.network.protocol.packet.PacketRegistry;
import io.lolyay.discordmsend.server.DstServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class NetworkServer {
    private final int port;
    private final DstServer dstServer;
    private final PacketRegistry registry;
    @Getter
    private final List<Connection> connections = new ArrayList<>();

    public NetworkServer(int port, PacketRegistry registry, DstServer dstServer) {
        this.port = port;
        this.dstServer = dstServer;
        this.registry = registry;
    }

    public void start() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            Connection connection = new Connection(ch, Enviroment.SERVER);
                            ch.pipeline()
                                    .addLast("frameDecoder", new ProtobufVarint32FrameDecoder())
                                    .addLast("frameEncoder", new ProtobufVarint32LengthFieldPrepender())
                                    .addLast("packetDecoder", new PacketDecoder(registry, connection, PacketDirection.SERVER_BOUND))
                                    .addLast("packetEncoder", new PacketEncoder(registry, connection, PacketDirection.CLIENT_BOUND))
                                    .addLast("handler", connection);
                            connection.setPhase(NetworkPhase.PRE_ENCRYPTION);
                            connection.setListener(new ServerPreEncryptionListener(connection, dstServer));
                            connections.add(connection);
                        }
                    });

            ChannelFuture f = b.bind(port).sync();
            System.out.println("Server started on port " + port);
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public void removeConnection(Connection connection) {
        connections.remove(connection);
        dstServer.removeClientByConnection(connection);
    }

}
