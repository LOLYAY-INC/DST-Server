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
import io.lolyay.discordmsend.server.Server;
import io.lolyay.discordmsend.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.concurrent.TimeUnit;

public class ServerPreEncryptionListener implements ServerPreEncryptionPacketListener {
    private final Connection connection;
    private final Server server;
    private byte[] nonce;
    private ConnectedClient client;

    public ServerPreEncryptionListener(Connection connection, Server server) {
        this.connection = connection;
        this.server = server;
        server.schedule(() -> {
            if(getConnection().getPhase() == NetworkPhase.PRE_ENCRYPTION)
                connection.disconnect("Encryption handshake failed: " + "Timeout");
        }, server.getEncryptionTimeout(), TimeUnit.SECONDS);
    }

    @Override
    public void onHandShake(HandShakeC2SPacket packet) {
        Logger.debug("HandShake Received");
        if(packet.protocolVersion() != Enviroment.PROTOCOL_VERSION){
            Logger.err("Protocol version Missmatch: SERVER:" + Enviroment.PROTOCOL_VERSION + " CLIENT: " + packet.protocolVersion());
            getConnection().disconnect("Protocol Version Missmatch!");
            throw new RuntimeException("Protocol version Missmatch");
        }
        if(!packet.apiKey().equals(server.getApiKey())){
            connection.disconnect("Encryption handshake failed: " + "Invalid API Key");
            return;
        }
        client = new ConnectedClient(packet.protocolVersion(), connection, server);
        server.addConnectedClient(client);

        nonce = NetworkEncryptionUtils.generateNonce(); // 16 bytes now
        PublicKey publicKey = server.getKeyPair().getPublic();
        byte[] pubBytes = RSA128Serializer.publicKeyToBytes(publicKey); // 128 bytes

        getConnection().send(new EncryptionRequestS2CPacket(pubBytes, nonce));
        Logger.debug("Sent ER");
    }

    @Override
    public void onEncryptionResponse(EncryptionResponseC2SPacket packet) {
        Logger.debug("Encryption Received");

        try {
            // 1. Decrypt the shared secret using the server's private RSA key
            byte[] decrypted = NetworkEncryptionUtils.crypt(
                    Cipher.DECRYPT_MODE,
                    server.getKeyPair().getPrivate(),
                    packet.sharedSecret() // the bytes sent by client
            );

            // 2. Un-XOR with the nonce
            for (int i = 0; i < decrypted.length; i++) {
                decrypted[i] ^= nonce[i]; // nonce is saved from handshake
            }
            //Why do we need to XOR? Shouldnt AES be enough?

            // 3. Recreate AES shared secret
            SecretKey sharedSecret = new javax.crypto.spec.SecretKeySpec(decrypted, "AES");

            // 4. Enable encryption for this connection
            connection.enableEncryption(sharedSecret);
            connection.setPhase(NetworkPhase.POST_ENCRYPTION);
            connection.setListener(new ServerPostEncryptionListener(server, connection, client));

            Logger.debug("Encryption handshake completed successfully!");

        } catch (GeneralSecurityException e) {
            Logger.err("Failed to decrypt client AES key.", e);
            connection.disconnect("Encryption handshake failed: " + e.getMessage());
        }
    }

    @Override
    public void onDisconnect(String reason) {
        Logger.info("Client disconnected During Encryption Handshake: " + reason);
        server.networkServer().removeConnection(connection);
    }

    @Override
    public Connection getConnection() {
        return connection;
    }
}
