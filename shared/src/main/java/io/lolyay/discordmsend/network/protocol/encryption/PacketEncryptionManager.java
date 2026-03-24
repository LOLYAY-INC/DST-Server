package io.lolyay.discordmsend.network.protocol.encryption;


import io.netty.buffer.ByteBuf;
import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;


public class PacketEncryptionManager {
    private final Cipher cipher;
    private byte[] inputBuffer = new byte[0];
    private byte[] outputBuffer = new byte[0];


    public PacketEncryptionManager(Cipher cipher) {
        this.cipher = cipher;
    }

    private byte[] toByteArray(ByteBuf buf) {
        int readableBytes = buf.readableBytes();
        if (this.inputBuffer.length < readableBytes) {
            this.inputBuffer = new byte[readableBytes];
        }
        buf.readBytes(this.inputBuffer, 0, readableBytes);
        return this.inputBuffer;
    }

    public void process(ByteBuf input, ByteBuf output) throws ShortBufferException {
        int readableBytes = input.readableBytes();
        byte[] bytes = toByteArray(input);

        int outputSize = this.cipher.getOutputSize(readableBytes);
        if (this.outputBuffer.length < outputSize) {
            this.outputBuffer = new byte[outputSize];
        }

        int writtenBytes = this.cipher.update(bytes, 0, readableBytes, this.outputBuffer);

        if (writtenBytes > 0) {
            output.writeBytes(this.outputBuffer, 0, writtenBytes);
        }
    }
}