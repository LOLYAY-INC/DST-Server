package io.lolyay.discordmsend.network.protocol.encryption;


import io.netty.buffer.ByteBuf;
import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;

/**
 * Manages the stateful Cipher and reusable buffers for encryption/decryption
 * to optimize performance and reduce memory allocations. This helper class
 * is used by both PacketEncryptor and PacketDecryptor.
 */
public class PacketEncryptionManager {
    private final Cipher cipher;
    private byte[] inputBuffer = new byte[0];
    private byte[] outputBuffer = new byte[0];

    /**
     * Constructs a new manager with a fully initialized Cipher.
     * @param cipher The AES stream cipher to use for transformations.
     */
    public PacketEncryptionManager(Cipher cipher) {
        this.cipher = cipher;
    }

    /**
     * A helper method to efficiently get the readable bytes from a Netty ByteBuf
     * into a reusable byte array.
     *
     * @param buf The buffer to read from.
     * @return A byte array containing the readable bytes from the buffer.
     */
    private byte[] toByteArray(ByteBuf buf) {
        int readableBytes = buf.readableBytes();
        // Resize the internal buffer only if it's too small
        if (this.inputBuffer.length < readableBytes) {
            this.inputBuffer = new byte[readableBytes];
        }
        buf.readBytes(this.inputBuffer, 0, readableBytes);
        return this.inputBuffer;
    }

    /**
     * Performs the cryptographic transformation (encryption or decryption) on the input data.
     *
     * @param input The buffer containing the source bytes (e.g., plaintext for encryption).
     * @param output The buffer to write the transformed bytes to (e.g., ciphertext).
     * @throws ShortBufferException if the output buffer is too small to hold the result.
     */
    public void process(ByteBuf input, ByteBuf output) throws ShortBufferException {
        int readableBytes = input.readableBytes();
        byte[] bytes = toByteArray(input);

        // Ask the cipher how large the output could be
        int outputSize = this.cipher.getOutputSize(readableBytes);
        // Resize the internal buffer only if it's too small
        if (this.outputBuffer.length < outputSize) {
            this.outputBuffer = new byte[outputSize];
        }

        // Perform the cipher.update operation, which encrypts/decrypts the data
        int writtenBytes = this.cipher.update(bytes, 0, readableBytes, this.outputBuffer);

        // Write the resulting bytes to the actual output ByteBuf
        if (writtenBytes > 0) {
            output.writeBytes(this.outputBuffer, 0, writtenBytes);
        }
    }
}