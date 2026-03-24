package io.lolyay.discordmsend.server.network;

import io.lolyay.discordmsend.network.Enviroment;
import io.lolyay.discordmsend.network.protocol.Connection;
import io.lolyay.discordmsend.network.protocol.NetworkPhase;
import io.lolyay.discordmsend.network.protocol.encryption.NetworkEncryptionUtils;
import io.lolyay.discordmsend.network.protocol.encryption.RSA128Serializer;
import io.lolyay.discordmsend.network.protocol.listeners.server.ServerPreEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.packets.C2S.preenc.EncryptionResponseC2SPacket;
import io.lolyay.discordmsend.network.protocol.packet.packets.C2S.preenc.HandShakeC2SPacket;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.preenc.EncryptionRequestS2CPacket;
import io.lolyay.discordmsend.server.DstServer;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ServerPreEncryptionListener implements ServerPreEncryptionPacketListener {
    private final Connection connection;
    private final DstServer dstServer;
    private byte[] nonce;
    private ConnectedClient client;

    public ServerPreEncryptionListener(Connection connection, DstServer dstServer) {
        this.connection = connection;
        this.dstServer = dstServer;
        dstServer.schedule(() -> {
            if(getConnection().getPhase() == NetworkPhase.PRE_ENCRYPTION)
                connection.disconnect("Encryption handshake failed: " + "Timeout");
        }, dstServer.getEncryptionTimeout(), TimeUnit.SECONDS);
    }

    @Override
    public void onHandShake(HandShakeC2SPacket packet) {
        if(packet.protocolVersion() != Enviroment.PROTOCOL_VERSION){
            log.error("Protocol version Missmatch: SERVER:" + Enviroment.PROTOCOL_VERSION + " CLIENT: " + packet.protocolVersion());
            getConnection().disconnect("Protocol Version Missmatch!");
            throw new RuntimeException("Protocol version Missmatch");
        }
        if(!packet.apiKey().equals(dstServer.getApiKey())){
            connection.disconnect("Encryption handshake failed: " + "Invalid API Key");
            return;
        }
        client = new ConnectedClient(packet.protocolVersion(), connection, dstServer);
        dstServer.addConnectedClient(client);

        nonce = NetworkEncryptionUtils.generateNonce();
        PublicKey publicKey = dstServer.getKeyPair().getPublic();
        byte[] pubBytes = RSA128Serializer.publicKeyToBytes(publicKey);

        getConnection().send(new EncryptionRequestS2CPacket(pubBytes, nonce));
        log.debug("Sent ER");
    }

    @Override
    public void onEncryptionResponse(EncryptionResponseC2SPacket packet) {
        log.debug("Encryption Received");

        try {
            byte[] decrypted = NetworkEncryptionUtils.crypt(
                    Cipher.DECRYPT_MODE,
                    dstServer.getKeyPair().getPrivate(),
                    packet.sharedSecret()
            );

            for (int i = 0; i < decrypted.length; i++) {
                decrypted[i] ^= nonce[i];
            }
            SecretKey sharedSecret = new javax.crypto.spec.SecretKeySpec(decrypted, "AES");

            connection.enableEncryption(sharedSecret);
            connection.setPhase(NetworkPhase.POST_ENCRYPTION);
            connection.setListener(new ServerPostEncryptionListener(dstServer, connection, client));

            log.debug("Encryption handshake completed successfully!");

        } catch (GeneralSecurityException e) {
            log.error("Failed to decrypt client AES key.", e);
            connection.disconnect("Encryption handshake failed: " + e.getMessage());
        }
    }

    @Override
    public void onDisconnect(String reason) {
        log.info("Client disconnected During Encryption Handshake: " + reason);
        dstServer.networkServer().removeConnection(connection);
    }

    @Override
    public Connection getConnection() {
        return connection;
    }
}
