package io.lolyay.discordmsend.client.net;

import io.lolyay.discordmsend.client.DstClient;
import io.lolyay.discordmsend.network.protocol.Connection;
import io.lolyay.discordmsend.network.protocol.NetworkPhase;
import io.lolyay.discordmsend.network.protocol.encryption.NetworkEncryptionUtils;
import io.lolyay.discordmsend.network.protocol.encryption.RSA128Serializer;
import io.lolyay.discordmsend.network.protocol.listeners.client.ClientPreEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.packets.C2S.preenc.EncryptionResponseC2SPacket;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.preenc.EncryptionRequestS2CPacket;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.security.PublicKey;

@Slf4j
public class ClientPreEncryptionListener implements ClientPreEncryptionPacketListener {
    private final Connection connection;
    private final DstClient dstClient;

    public ClientPreEncryptionListener(Connection connection, DstClient dstClient) {
        this.connection = connection;
        this.dstClient = dstClient;
    }

    @Override
    public void onEncryptionRequest(EncryptionRequestS2CPacket packet) {
        log.debug("Client: Received Encryption Request.");
        try {
            PublicKey serverPublicKey = RSA128Serializer.bytesToPublicKey(packet.publicKey());

            byte[] nonce = packet.nonce();
            SecretKey sharedSecret = NetworkEncryptionUtils.createAesKey();
            byte[] encodedSecret = sharedSecret.getEncoded();

            for (int i = 0; i < encodedSecret.length; i++) {
                encodedSecret[i] ^= nonce[i];
            }

            byte[] encryptedSecret = NetworkEncryptionUtils.crypt(
                    Cipher.ENCRYPT_MODE, serverPublicKey, encodedSecret
            );

            connection.send(new EncryptionResponseC2SPacket(encryptedSecret));

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
        log.debug("Client: Disconnected during encryption: " + reason);
    }


    @Override
    public Connection getConnection() {
        return connection;
    }
}