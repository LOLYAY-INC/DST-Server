package io.lolyay.discordmsend.util.nbt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.nio.charset.StandardCharsets;

public final class NbtUtils {

    /**
     * Corrects a ByteBuf containing NBT data that has a root tag ID but is missing the root name.
     * It reads from the original buffer and writes to a new buffer without releasing the original.
     * The Netty pipeline is responsible for managing the original buffer's lifecycle.
     *
     * @param originalBuffer The buffer containing the Minecraft Network NBT data.
     * @param rootName The name to insert for the root tag (e.g., an empty string "").
     * @return A new ByteBuf with the correctly formatted NBT structure.
     */
    public static ByteBuf convertNetworkNbtToNormalNbt(ByteBuf originalBuffer, String rootName) {
        if (!originalBuffer.isReadable()) {
            // If the buffer is not readable, we should not attempt to read from it.
            // Returning the original allows the pipeline to handle it correctly.
            return originalBuffer;
        }

        ByteBufAllocator allocator = originalBuffer.alloc();
        ByteBuf newBuffer = allocator.buffer();

        // Write the tag ID from the original to the new.
        newBuffer.writeByte(originalBuffer.getByte(originalBuffer.readerIndex()));

        // Write the name we want to insert.
        byte[] nameBytes = rootName.getBytes(StandardCharsets.UTF_8);
        newBuffer.writeShort(nameBytes.length);
        newBuffer.writeBytes(nameBytes);

        // Advance the reader index on the original buffer past the tag ID we already "read".
        originalBuffer.skipBytes(1);

        // Write the entire remaining payload from the original buffer to the new one.
        newBuffer.writeBytes(originalBuffer);

        // Return the new buffer. The caller is now responsible for this buffer,
        // and Netty will handle releasing the original one.
        return newBuffer;
    }

    /**
     * Strips the root name from a standard NBT structure. This implementation is also
     * corrected to not interfere with Netty's buffer management.
     */
    public static ByteBuf stripNbtRootName(ByteBuf fullNbtBuffer) {
        if (fullNbtBuffer.readableBytes() < 3) {
            return fullNbtBuffer;
        }

        ByteBufAllocator allocator = fullNbtBuffer.alloc();
        ByteBuf strippedBuffer = allocator.buffer();

        // Get data without advancing the reader index of the original buffer yet.
        byte tagId = fullNbtBuffer.getByte(fullNbtBuffer.readerIndex());
        short nameLength = fullNbtBuffer.getShort(fullNbtBuffer.readerIndex() + 1);

        // Write the tag ID to our new buffer.
        strippedBuffer.writeByte(tagId);

        // Calculate the starting position of the payload.
        int payloadOffset = fullNbtBuffer.readerIndex() + 1 + 2 + nameLength;

        // Write the payload by creating a slice. This is very efficient as it avoids copying.
        strippedBuffer.writeBytes(fullNbtBuffer, payloadOffset, fullNbtBuffer.readableBytes() - (1 + 2 + nameLength));

        return strippedBuffer;
    }
}