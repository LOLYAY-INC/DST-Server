package io.lolyay.discordmsend.network.protocol;


import io.lolyay.discordmsend.network.Enviroment;
import io.lolyay.discordmsend.network.protocol.encryption.NetworkEncryptionUtils;
import io.lolyay.discordmsend.network.protocol.encryption.PacketDecryptor;
import io.lolyay.discordmsend.network.protocol.encryption.PacketEncryptor;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.PacketListener;
import io.lolyay.discordmsend.network.protocol.packet.packets.C2S.preenc.HandShakeC2SPacket;
import io.lolyay.discordmsend.network.types.ClientFeatures;
import io.lolyay.discordmsend.util.logging.Logger;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;

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

    public Connection(Channel channel, Enviroment environment) { // for server
        this.channel = channel;
        this.APIKEY = null;
        this.environment = environment;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        listener.onDisconnect("Connection closed");
    }

    public boolean isActive(){
        return channel.isActive();
    }

    // New constructor for the client to pass in required data
    public Connection(Channel channel, Enviroment environment, int protocolVersion, String host, int port, String APIKEY) {
        this.channel = channel;
        this.APIKEY = APIKEY;
        this.environment = environment;
        this.protocolVersion = protocolVersion;
        this.host = host;
        this.port = port;
    }

    public long getTimeSinceLastPacketRec(){
        return (System.currentTimeMillis() - timeSinceLastPacket);
    }

    /**
     * This is the concrete method that is called ONCE the connection is active.
     * This is the perfect place to send the initial packets.
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // Only the client should send initial packets. The server waits.
        Logger.warn( environment.name() + " Conncted .");
        if (environment != Enviroment.CLIENT) {
            return;
        }

        // 1. Create and send the Handshake packet
        HandShakeC2SPacket handshakePacket = new HandShakeC2SPacket(protocolVersion, host, APIKEY, port);
        setPhase(NetworkPhase.PRE_ENCRYPTION);
        send(handshakePacket);
        Logger.debug("Client: Sent handshake, waiting for encryption packet");
    }

    public void setPhase(NetworkPhase phase) {
        this.phase = phase;
        Logger.log("Connection to " + channel.remoteAddress() + " transitioned to phase " + phase + " on " + environment);
    }

    public void enableEncryption(SecretKey secretKey) throws GeneralSecurityException {
        Cipher encryptCipher = NetworkEncryptionUtils.createStreamCipher(Cipher.ENCRYPT_MODE, secretKey);
        Cipher decryptCipher = NetworkEncryptionUtils.createStreamCipher(Cipher.DECRYPT_MODE, secretKey);

        ChannelPipeline pipeline = this.channel.pipeline();


        pipeline.addBefore("frameDecoder", "decryptor", new PacketDecryptor(decryptCipher));
        pipeline.addBefore("frameEncoder", "encryptor", new PacketEncryptor(encryptCipher));

        Logger.log("Encryption enabled. Crypto handlers correctly added to pipeline on " + environment.name());
    }

    public Enviroment getEnvironment() {
        return environment;
    }


    public void setListener(PacketListener listener) {
        this.listener = listener;
    }

    public NetworkPhase getPhase() {
        return phase;
    }

    public void send(Packet<?> packet) {
        if (packet == null) {
            return;
        }
        if (channel.isActive()) {
            channel.writeAndFlush(packet).addListener(future -> {
                if (future.isSuccess()) {
                } else {
                    Logger.err("Failed to send packet: " + packet.getClass().getSimpleName());
                    future.cause().printStackTrace();
                }
            });
        } else {
            Logger.warn("Channel not active, cannot send: " + packet.getClass().getSimpleName());
        }
    }

    public void disconnect(String reason) {
        if (channel.isActive()) {
            Logger.log("Disconnecting " + channel.remoteAddress() + ": " + reason);
            channel.close();
        }listener.onDisconnect(reason);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet<?> packet) {
        timeSinceLastPacket = System.currentTimeMillis();
        
        if (listener != null)
                dispatch(packet, this.listener);
        else
            Logger.warn("[CONNECTION] No listener set for packet: " + packet.getClass().getSimpleName());
    }
    /**
     * A private helper method to handle the unsafe generic cast required by the
     * Visitor pattern. By using raw types (Packet without <?>) and suppressing
     * the warning here, we contain the "unsafe" part of the code in one place.
     * Our design guarantees this is safe because the listener is always updated
     * according to the current NetworkPhase.
     */
    @SuppressWarnings("unchecked")
    private static void dispatch(Packet packet, PacketListener listener) {
        // Debug logging for SetSourceC2SPacket
        packet.apply(listener);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof java.io.IOException) {
            System.err.println("Connection I/O error for " + channel.remoteAddress() + ": " + cause.getMessage());
        } else {
            System.err.println("An exception occurred in connection " + channel.remoteAddress());
            cause.printStackTrace();
        }
        disconnect("Internal error");
    }
}
