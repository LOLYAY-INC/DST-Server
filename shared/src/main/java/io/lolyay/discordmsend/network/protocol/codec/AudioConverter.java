package io.lolyay.discordmsend.network.protocol.codec;


public class AudioConverter {
    public static void convertToByteArray(short[] src, byte[] target) {
        for (int i = 0; i < src.length; i++) {
            int byteIdx = i << 1; // Faster i * 2
            target[byteIdx] = (byte) (src[i] & 0xFF);
            target[byteIdx + 1] = (byte) ((src[i] >> 8) & 0xFF);
        }
    }
    public static void convertToShortArray(byte[] src, short[] target) {
        for (int i = 0; i < target.length; i++) {
            int byteIdx = i << 1;
            target[i] = (short) ((src[byteIdx] & 0xFF) |
                    ((src[byteIdx + 1] & 0xFF) << 8));
        }
    }
}