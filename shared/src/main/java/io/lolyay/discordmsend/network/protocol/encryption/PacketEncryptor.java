package io.lolyay.discordmsend.network.protocol.encryption;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import javax.crypto.Cipher;

/**
 * A Netty handler that encrypts outgoing ByteBufs.
 * This handler is inserted into the pipeline dynamically after the handshake.
 */
public class PacketEncryptor extends MessageToByteEncoder<ByteBuf> {
    private final PacketEncryptionManager manager;

    public PacketEncryptor(Cipher cipher) {
        this.manager = new PacketEncryptionManager(cipher);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
        this.manager.process(in, out);
    }
}