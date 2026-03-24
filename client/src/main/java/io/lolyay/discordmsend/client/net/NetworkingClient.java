package io.lolyay.discordmsend.client.net;


import io.lolyay.discordmsend.client.DstClient;
import io.lolyay.discordmsend.network.Enviroment;
import io.lolyay.discordmsend.network.protocol.Connection;
import io.lolyay.discordmsend.network.protocol.NetworkPhase;
import io.lolyay.discordmsend.network.protocol.coder.PacketDecoder;
import io.lolyay.discordmsend.network.protocol.coder.PacketEncoder;
import io.lolyay.discordmsend.network.protocol.packet.PacketDirection;
import io.lolyay.discordmsend.network.protocol.packet.PacketRegistry;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NetworkingClient {
    private final String host;
    private final int port;
    private final int protocolVersion;
    public Connection connection;
    public boolean connected = false;
    private final PacketRegistry registry;
    private final DstClient dstClient;

    public NetworkingClient(int protocolVersion, String host, int port, PacketRegistry registry, DstClient dstClient) {
        this.host = host;
        this.dstClient = dstClient;
        this.protocolVersion = protocolVersion;
        this.port = port;
        this.registry = registry;
    }

    /**
     * Establishes a connection to the server and begins the login process.
     * This method blocks until the connection is closed.
     *
     * @throws Exception If an error occurs connecting to the server or during the login process.
     */

    public void disconnect(String string){
        if (connection != null) {
            connection.disconnect(string);
            connection = null;
        }
    }
    public void start() throws Exception {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            connection = new Connection(ch, Enviroment.CLIENT, protocolVersion, host, port, dstClient.getApiKey());

                            ch.pipeline()
                                    .addLast("frameDecoder", new ProtobufVarint32FrameDecoder())
                                    .addLast("packetDecoder", new PacketDecoder(registry, connection, PacketDirection.CLIENT_BOUND))

                                    .addLast("frameEncoder", new ProtobufVarint32LengthFieldPrepender())
                                    .addLast("packetEncoder", new PacketEncoder(registry, connection, PacketDirection.SERVER_BOUND))

                                    .addLast("handler", connection);

                            ClientPreEncryptionListener listener = new ClientPreEncryptionListener(connection, dstClient);
                            connection.setListener(listener);
                            connection.setPhase(NetworkPhase.PRE_ENCRYPTION);
                        }
                    });

            ChannelFuture f = b.connect(host, port).sync();
            log.info("Client connected to " + host + ":" + port);
            connected = true;

            f.channel().closeFuture().sync();
            connected = false;
        } finally {
            workerGroup.shutdownGracefully();
        }
    }
}