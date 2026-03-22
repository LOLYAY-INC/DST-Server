package io.lolyay.discordmsend.util.nbt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.nio.charset.StandardCharsets;

public final class NbtUtils {
    public static ByteBuf convertNetworkNbtToNormalNbt(ByteBuf originalBuffer, String rootName) {
        if (!originalBuffer.isReadable()) {
            return originalBuffer;
        }

        ByteBufAllocator allocator = originalBuffer.alloc();
        ByteBuf newBuffer = allocator.buffer();

        newBuffer.writeByte(originalBuffer.getByte(originalBuffer.readerIndex()));

        byte[] nameBytes = rootName.getBytes(StandardCharsets.UTF_8);
        newBuffer.writeShort(nameBytes.length);
        newBuffer.writeBytes(nameBytes);

        originalBuffer.skipBytes(1);

        newBuffer.writeBytes(originalBuffer);

        return newBuffer;
    }

    public static ByteBuf stripNbtRootName(ByteBuf fullNbtBuffer) {
        if (fullNbtBuffer.readableBytes() < 3) {
            return fullNbtBuffer;
        }

        ByteBufAllocator allocator = fullNbtBuffer.alloc();
        ByteBuf strippedBuffer = allocator.buffer();

        byte tagId = fullNbtBuffer.getByte(fullNbtBuffer.readerIndex());
        short nameLength = fullNbtBuffer.getShort(fullNbtBuffer.readerIndex() + 1);

        strippedBuffer.writeByte(tagId);

        int payloadOffset = fullNbtBuffer.readerIndex() + 1 + 2 + nameLength;

        strippedBuffer.writeBytes(fullNbtBuffer, payloadOffset, fullNbtBuffer.readableBytes() - (1 + 2 + nameLength));

        return strippedBuffer;
    }
}