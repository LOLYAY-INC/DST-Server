package io.lolyay.discordmsend.network.protocol.encryption;


import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.*;

public class NetworkEncryptionUtils {

    public static KeyPair createRsaKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to create RSA key pair", e);
        }
    }

    private static final int SIZE = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Generate a random 128-byte nonce */
    public static byte[] generateNonce() {
        byte[] nonce = new byte[SIZE];
        RANDOM.nextBytes(nonce);
        return nonce;
    }

    /** XOR two 128-byte arrays */
    public static byte[] xor(byte[] a, byte[] b) {
        if (a.length != SIZE || b.length != SIZE) {
            throw new IllegalArgumentException("Both arrays must be 128 bytes");
        }
        byte[] result = new byte[SIZE];
        for (int i = 0; i < SIZE; i++) {
            result[i] = (byte) (a[i] ^ b[i]);
        }
        return result;
    }
    public static SecretKey createAesKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to create AES key", e);
        }
    }

    public static byte[] crypt(int opMode, Key key, byte[] data) throws GeneralSecurityException {
        // Explicitly specify the padding as per the protocol documentation
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(opMode, key);
        return cipher.doFinal(data);
    }

    public static SecretKey decryptSecretKey(PrivateKey privateKey, byte[] encryptedSecretKey) throws GeneralSecurityException {
        byte[] decryptedBytes = crypt(Cipher.DECRYPT_MODE, privateKey, encryptedSecretKey);
        return new SecretKeySpec(decryptedBytes, "AES");
    }

    public static Cipher createStreamCipher(int opMode, Key key) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
        IvParameterSpec iv = new IvParameterSpec(key.getEncoded());
        cipher.init(opMode, key, iv);
        return cipher;
    }

    public static byte[] computeServerIdHash(String serverId, SecretKey sharedSecret, PublicKey publicKey) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(serverId.getBytes("ISO_8859_1"));
        digest.update(sharedSecret.getEncoded());
        digest.update(publicKey.getEncoded());
        return digest.digest();
    }
}