package io.lolyay.discordmsend.client.net;

import io.lolyay.discordmsend.client.DstClient;
import io.lolyay.discordmsend.network.protocol.Connection;
import io.lolyay.discordmsend.network.protocol.NetworkPhase;
import io.lolyay.discordmsend.network.protocol.encryption.NetworkEncryptionUtils;
import io.lolyay.discordmsend.network.protocol.encryption.RSA128Serializer;
import io.lolyay.discordmsend.network.protocol.listeners.client.ClientPreEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.packets.C2S.preenc.EncryptionResponseC2SPacket;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.preenc.EncryptionRequestS2CPacket;
import io.lolyay.discordmsend.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.security.PublicKey;

/**
 * A client-side listener that handles the LOGIN phase, including optional
 * authentication and encryption, according to the official protocol.
 */
public class ClientPreEncryptionListener implements ClientPreEncryptionPacketListener {
    private final Connection connection;
    private final DstClient dstClient;

    public ClientPreEncryptionListener(Connection connection, DstClient dstClient) {
        this.connection = connection;
        this.dstClient = dstClient;
    }

    @Override
    public void onEncryptionRequest(EncryptionRequestS2CPacket packet) {
        Logger.debug("Client: Received Encryption Request.");
        try {
            PublicKey serverPublicKey = RSA128Serializer.bytesToPublicKey(packet.publicKey());

            byte[] nonce = packet.nonce(); // 16 bytes
            SecretKey sharedSecret = NetworkEncryptionUtils.createAesKey(); // 16 bytes
            byte[] encodedSecret = sharedSecret.getEncoded(); // 16 bytes

            // XOR AES key with nonce (all 16 bytes)
            for (int i = 0; i < encodedSecret.length; i++) {
                encodedSecret[i] ^= nonce[i];
            }

            // Encrypt with RSA (1024-bit key)
            byte[] encryptedSecret = NetworkEncryptionUtils.crypt(
                    Cipher.ENCRYPT_MODE, serverPublicKey, encodedSecret
            );

            // Send encryption response
            Logger.debug("Client: Sending encryption response.");
            connection.send(new EncryptionResponseC2SPacket(encryptedSecret));

            // Enable AES encryption for the connection
            Logger.debug("Client: Enabling encryption.");
            connection.setPhase(NetworkPhase.POST_ENCRYPTION);
            connection.setListener(new ClientPostEncryptionListener(connection, dstClient));
            connection.enableEncryption(sharedSecret);

        } catch (GeneralSecurityException e) {
            System.err.println("A critical error occurred during the encryption handshake.");
            e.printStackTrace();
            connection.disconnect("Encryption handshake failed: " + e.getMessage());
        }
    }


    @Override
    public void onDisconnect(String reason) {
        Logger.debug("Client: Disconnected during encryption: " + reason);
        //
    }


    @Override
    public Connection getConnection() {
        return connection;
    }
}