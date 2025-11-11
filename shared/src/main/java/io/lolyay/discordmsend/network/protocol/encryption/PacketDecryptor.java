package io.lolyay.discordmsend.network.protocol.encryption;


import io.lolyay.discordmsend.util.logging.BufDumper;
import io.lolyay.discordmsend.util.logging.Logger;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import javax.crypto.Cipher;
import java.util.List;

/**
 * A Netty handler that decrypts incoming ByteBufs.
 * This handler is inserted into the pipeline dynamically after the handshake.
 */
public class PacketDecryptor extends ByteToMessageDecoder {
    private final PacketEncryptionManager manager;

    public PacketDecryptor(Cipher cipher) {
        this.manager = new PacketEncryptionManager(cipher);
    }
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int readable = in.readableBytes();

        if (readable == 0) {
            return;
        }
        
        // Create a new buffer to hold the decrypted data
        ByteBuf decrypted = ctx.alloc().heapBuffer(readable);
        this.manager.process(in, decrypted);
        

        // Add the decrypted buffer to the output list for the next handler
        out.add(decrypted);
    }
}
