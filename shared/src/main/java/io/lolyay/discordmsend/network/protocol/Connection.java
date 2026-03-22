package io.lolyay.discordmsend.network.protocol;


import io.lolyay.discordmsend.network.Enviroment;
import io.lolyay.discordmsend.network.protocol.encryption.NetworkEncryptionUtils;
import io.lolyay.discordmsend.network.protocol.encryption.PacketDecryptor;
import io.lolyay.discordmsend.network.protocol.encryption.PacketEncryptor;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketListener;
import io.lolyay.discordmsend.network.protocol.packet.packets.C2S.preenc.HandShakeC2SPacket;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
@Slf4j
@Getter
@Setter
public class Connection extends SimpleChannelInboundHandler<Packet<?>> {

    private final Channel channel;
    private PacketListener listener;
    private NetworkPhase phase;
    private final Enviroment environment;
    private final String APIKEY;

    public long timeSinceLastPacket = System.currentTimeMillis();

    private int protocolVersion;
    private String host;
    private int port;

    public Connection(Channel channel, Enviroment environment) {
        this.channel = channel;
        this.APIKEY = null;
        this.environment = environment;
    }

    public Connection(Channel channel, Enviroment environment, int protocolVersion, String host, int port, String APIKEY) {
        this.channel = channel;
        this.APIKEY = APIKEY;
        this.environment = environment;
        this.protocolVersion = protocolVersion;
        this.host = host;
        this.port = port;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        listener.onDisconnect("Connection closed");
    }

    public boolean isActive(){
        return channel.isActive();
    }


    public long getTimeSinceLastPacketRec(){
        return (System.currentTimeMillis() - timeSinceLastPacket);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        if (environment != Enviroment.CLIENT) {
            return;
        }

        HandShakeC2SPacket handshakePacket = new HandShakeC2SPacket(protocolVersion, host, APIKEY, port);
        setPhase(NetworkPhase.PRE_ENCRYPTION);
        send(handshakePacket);
    }


    public void enableEncryption(SecretKey secretKey) throws GeneralSecurityException {
        Cipher encryptCipher = NetworkEncryptionUtils.createStreamCipher(Cipher.ENCRYPT_MODE, secretKey);
        Cipher decryptCipher = NetworkEncryptionUtils.createStreamCipher(Cipher.DECRYPT_MODE, secretKey);

        ChannelPipeline pipeline = this.channel.pipeline();

        pipeline.addBefore("frameDecoder", "decryptor", new PacketDecryptor(decryptCipher));
        pipeline.addBefore("frameEncoder", "encryptor", new PacketEncryptor(encryptCipher));
    }



    public void send(Packet<?> packet) {
        if (packet == null) {
            return;
        }
        if (channel.isActive()) {
            channel.writeAndFlush(packet).addListener(future -> {
                if (!future.isSuccess()) {
                    log.error("Failed to send packet: {}", packet.getClass().getSimpleName());
                    future.cause().printStackTrace();
                }
            });
        } else {
            log.warn("Channel not active, cannot send: {}", packet.getClass().getSimpleName());
        }
    }

    public void disconnect(String reason) {
        if (channel.isActive()) {
            log.info("Disconnecting {}: {}", channel.remoteAddress(), reason);
            channel.close();
        }
        listener.onDisconnect(reason);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet<?> packet) {
        timeSinceLastPacket = System.currentTimeMillis();
        
        if (listener != null)
                dispatch(packet, this.listener);
        else
            log.warn("No listener set for packet: {}", packet.getClass().getSimpleName());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void dispatch(Packet packet, PacketListener listener) {
        packet.apply(listener);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof java.io.IOException) {
            log.error("Connection I/O error for {}: {}", channel.remoteAddress(), cause.getMessage());
        } else {
            log.error("An exception occurred in connection {}", channel.remoteAddress());
            cause.printStackTrace();
        }
        disconnect("Internal error");
    }
}
