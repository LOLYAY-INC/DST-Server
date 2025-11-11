package io.lolyay.discordmsend.network.protocol.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * A specialized, lightweight NBT parser that reads a "Network NBT" structure from a
 * source buffer and copies it verbatim into a new, self-contained destination buffer.
 * It correctly handles the modern (post-1.20.2) protocol where the root TAG_COMPOUND is unnamed.
 */
public class MiniNBT {
    // NBT Tag Type IDs
    private static final byte TAG_END_ID = 0x00;
    private static final byte TAG_BYTE_ID = 0x01;
    private static final byte TAG_SHORT_ID = 0x02;
    private static final byte TAG_INT_ID = 0x03;
    private static final byte TAG_LONG_ID = 0x04;
    private static final byte TAG_FLOAT_ID = 0x05;
    private static final byte TAG_DOUBLE_ID = 0x06;
    private static final byte TAG_BYTE_ARRAY_ID = 0x07;
    private static final byte TAG_STRING_ID = 0x08;
    private static final byte TAG_LIST_ID = 0x09;
    private static final byte TAG_COMPOUND_ID = 0x0a;
    private static final byte TAG_INT_ARRAY_ID = 0x0b;
    private static final byte TAG_LONG_ARRAY_ID = 0x0c;

    private final ByteBuf source;
    private final ByteBuf destination = Unpooled.buffer();

    public MiniNBT(ByteBuf source) {
        this.source = source;
    }

    /**
     * Parses the Network NBT structure. It assumes the first byte is the
     * root tag ID, which is expected to be TAG_COMPOUND and is UNNAMED.
     * @return A new ByteBuf containing only the parsed NBT data.
     */
    public ByteBuf parse() {
        byte rootTagId = source.readByte();
        destination.writeByte(rootTagId);

        if (rootTagId == TAG_END_ID) {
            // An empty NBT structure is valid.
            return destination;
        }
        if( rootTagId == TAG_STRING_ID){
            copyTagPayload(TAG_STRING_ID);
            return destination;
        }

        // According to the modern network protocol, the root tag MUST be a compound tag.
        if (rootTagId != TAG_COMPOUND_ID) {
            throw new IllegalStateException("Network NBT root tag must be TAG_COMPOUND, but was " + rootTagId);
        }

        // --- THE DEFINITIVE FIX ---
        // For a network NBT structure, the root tag is unnamed.
        // We do NOT copy a tag name. We immediately start copying the compound's payload.
        copyCompoundPayload();

        // Once the root compound's payload is fully copied (ending in its own TAG_END),
        // our job is done. We return the result.
        return destination;
    }

    /**
     * Copies the payload of a compound tag. It repeatedly copies inner tags
     * (which ARE named) until it encounters this compound's TAG_END.
     */
    private void copyCompoundPayload() {
        while (true) {
            byte tagId = source.readByte();
            destination.writeByte(tagId);

            if (tagId == TAG_END_ID) {
                break; // Found the end of this compound tag.
            }

            // Inner tags are always named.
            copyTagName();
            copyTagPayload(tagId);
        }
    }

    private void copyTagName() {
        short nameLength = source.readShort();
        destination.writeShort(nameLength);
        destination.writeBytes(source, nameLength);
    }

    /**
     * Copies the data payload for a specific tag type.
     * This method does NOT handle the tag's ID or name, only its data.
     * @param tagId The ID of the tag type to copy.
     */
    private void copyTagPayload(byte tagId) {
        switch (tagId) {
            case TAG_END_ID: // Should not be handled here, but as a safeguard.
                break;
            case TAG_BYTE_ID:
                destination.writeByte(source.readByte());
                break;
            case TAG_SHORT_ID:
                destination.writeShort(source.readShort());
                break;
            case TAG_INT_ID:
                destination.writeInt(source.readInt());
                break;
            case TAG_LONG_ID:
                destination.writeLong(source.readLong());
                break;
            case TAG_FLOAT_ID:
                destination.writeFloat(source.readFloat());
                break;
            case TAG_DOUBLE_ID:
                destination.writeDouble(source.readDouble());
                break;
            case TAG_BYTE_ARRAY_ID: {
                int length = source.readInt();
                destination.writeInt(length);
                destination.writeBytes(source, length);
                break;
            }
            case TAG_STRING_ID: {
                short length = source.readShort();
                destination.writeShort(length);
                destination.writeBytes(source, length);
                break;
            }
            case TAG_LIST_ID: {
                byte listTagId = source.readByte();
                destination.writeByte(listTagId);
                int length = source.readInt();
                destination.writeInt(length);
                for (int i = 0; i < length; i++) {
                    // List items are unnamed payloads.
                    copyTagPayload(listTagId);
                }
                break;
            }
            case TAG_COMPOUND_ID:
                // A nested compound tag.
                copyCompoundPayload();
                break;
            case TAG_INT_ARRAY_ID: {
                int length = source.readInt();
                destination.writeInt(length);
                for (int i = 0; i < length; i++) {
                    destination.writeInt(source.readInt());
                }
                break;
            }
            case TAG_LONG_ARRAY_ID: {
                int length = source.readInt();
                destination.writeInt(length);
                for (int i = 0; i < length; i++) {
                    destination.writeLong(source.readLong());
                }
                break;
            }
            default:
                throw new IllegalStateException("Unknown NBT tag type: " + tagId);
        }
    }
}