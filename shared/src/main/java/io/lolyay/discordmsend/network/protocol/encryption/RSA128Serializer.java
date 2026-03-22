package io.lolyay.discordmsend.network.protocol.encryption;

import java.math.BigInteger;
import java.security.*;
import java.security.spec.*;

public class RSA128Serializer {

    private static final BigInteger FIXED_EXPONENT = BigInteger.valueOf(65537);

    public static byte[] publicKeyToBytes(PublicKey pubKey)  {
        java.security.interfaces.RSAPublicKey rsaPub = (java.security.interfaces.RSAPublicKey) pubKey;
        BigInteger modulus = rsaPub.getModulus();
        byte[] modBytes = modulus.toByteArray();

        byte[] result = new byte[128];
        int copyStart = Math.max(0, modBytes.length - 128);
        int copyLength = Math.min(modBytes.length, 128);
        System.arraycopy(modBytes, copyStart, result, 128 - copyLength, copyLength);

        return result;
    }

    public static PublicKey bytesToPublicKey(byte[] bytes)  {
        if (bytes.length != 128) {
            throw new IllegalArgumentException("Public key must be exactly 128 bytes");
        }
        BigInteger modulus = new BigInteger(1, bytes);
        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, FIXED_EXPONENT);
        KeyFactory kf = null;
        try {
            kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
