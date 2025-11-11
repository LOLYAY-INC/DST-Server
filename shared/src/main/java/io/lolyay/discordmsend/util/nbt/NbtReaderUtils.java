package io.lolyay.discordmsend.util.nbt;

import dev.dewy.nbt.api.Tag;
import dev.dewy.nbt.tags.primitive.*;
import io.lolyay.discordmsend.util.logging.Logger;
import io.netty.buffer.ByteBuf;

import java.io.IOException;

/**
 * A utility to measure the size of a single NBT tag from a network stream,
 * respecting the modern (1.20.2+) format where the root compound tag has no name.
 */
public final class NbtReaderUtils {

    private static final int MAX_DEPTH = 512;

    /**
     * Calculates the size of the next NBT tag in the buffer without advancing the reader index.
     * It expects the data to start with the root tag's type ID, followed immediately by its payload.
     *
     * @param buf The buffer containing the NBT data.
     * @return The size in bytes of the complete NBT tag.
     * @throws IOException if the data is malformed or an unknown tag type is encountered.
     */
    public static int getNextTagSize(ByteBuf buf) throws IOException {
        int originalReaderIndex = buf.readerIndex();
        try {
            if (!buf.isReadable()) {
                return 0; // No data, size is 0.
            }

            // 1. Read the root tag's type ID.
            byte rootType = buf.readByte();

            // 2. If it's TAG_End, the total size is just 1 byte.
            if (rootType == 0) {
                return 1;
            }

            // 3. Skip the payload corresponding to the root type.
            skipPayload(buf, rootType, 0);

            // 4. The total size is the number of bytes we just read/skipped.
            return buf.readerIndex() - originalReaderIndex;
        } finally {
            // 5. Always reset the reader index to its original position.
            buf.readerIndex(originalReaderIndex);
        }
    }

    /**
     * Skips a single named tag (type, name, payload), used for children of a TAG_Compound.
     */
    private static void skipNamedTag(ByteBuf buf, int depth) throws IOException {
        if (!buf.isReadable()) {
            throw new IOException("Cannot read NBT tag type, not enough bytes available.");
        }
        byte type = buf.readByte();
        if (type == 0) {
            return; // TAG_End has no name or payload.
        }

        // Skip name
        if (!buf.isReadable(2)) throw new IOException("Cannot read NBT tag name length.");
        int nameLength = buf.readUnsignedShort();
        if (nameLength < 0) throw new IOException("Invalid negative name length: " + nameLength);
        if (!buf.isReadable(nameLength)) throw new IOException("Not enough bytes for tag name of length " + nameLength);
        buf.skipBytes(nameLength);

        skipPayload(buf, type, depth + 1);
    }

    /**
     * Skips the payload of a tag, given its type ID.
     */
    private static void skipPayload(ByteBuf buf, byte type, int depth) throws IOException {
        if (depth >= MAX_DEPTH) {
            throw new IOException("NBT structure exceeds maximum depth of " + MAX_DEPTH);
        }

        switch (type) {
            case 0: // TAG_End
                break;
            case 1: buf.skipBytes(1); break; // Byte
            case 2: buf.skipBytes(2); break; // Short
            case 3: buf.skipBytes(4); break; // Int
            case 4: buf.skipBytes(8); break; // Long
            case 5: buf.skipBytes(4); break; // Float
            case 6: buf.skipBytes(8); break; // Double
            case 7: // TAG_Byte_Array
                int byteArrayLength = buf.readInt();
                if (byteArrayLength < 0) throw new IOException("Negative length for TAG_Byte_Array: " + byteArrayLength);
                buf.skipBytes(byteArrayLength);
                break;
            case 8: // TAG_String
                int stringLength = buf.readUnsignedShort();
                buf.skipBytes(stringLength);
                break;
            case 9: // TAG_List
                byte listType = buf.readByte();
                int listLength = buf.readInt();
                if (listLength < 0) throw new IOException("Negative length for TAG_List: " + listLength);
                int primitiveSize = getPrimitiveSize(listType);
                if (primitiveSize > 0) {
                    buf.skipBytes(listLength * primitiveSize);
                } else if (listLength > 0) {
                    for (int i = 0; i < listLength; i++) {
                        skipPayload(buf, listType, depth + 1);
                    }
                }
                break;
            case 10: // TAG_Compound
                while (true) {
                    byte nextTagType = buf.getByte(buf.readerIndex());
                    if (nextTagType == 0) {
                        buf.skipBytes(1); // Consume the TAG_End.
                        break;
                    }
                    skipNamedTag(buf, depth);
                }
                break;
            case 11: // TAG_Int_Array
                int intArrayLength = buf.readInt();
                if (intArrayLength < 0) throw new IOException("Negative length for TAG_Int_Array: " + intArrayLength);
                buf.skipBytes( intArrayLength * 4);
                break;
            case 12: // TAG_Long_Array
                int longArrayLength = buf.readInt();
                if (longArrayLength < 0) throw new IOException("Negative length for TAG_Long_Array: " + longArrayLength);
                buf.skipBytes( longArrayLength * 8);
                break;
            default:
                throw new IOException("Unknown NBT tag type: " + type);
        }
    }

    private static int getPrimitiveSize(byte type) {
        switch (type) {
            case 1: return 1; case 2: return 2;
            case 3: return 4; case 4: return 8;
            case 5: return 4; case 6: return 8;
            default: return 0;
        }
    }
}