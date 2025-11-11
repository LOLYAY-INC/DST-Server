package io.lolyay.discordmsend.network.protocol.codec;



import dev.dewy.nbt.Nbt;
import dev.dewy.nbt.api.Tag;
import dev.dewy.nbt.api.registry.TagTypeRegistry;
import dev.dewy.nbt.io.NbtReader;
import dev.dewy.nbt.tags.collection.CompoundTag;
import dev.dewy.nbt.tags.primitive.StringTag;
import io.lolyay.discordmsend.util.logging.Logger;
import io.lolyay.discordmsend.util.nbt.NbtUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ByteProcessor;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;


/**
 * A definitive implementation of a Minecraft-protocol-aware ByteBuf decorator.
 * Implements all major data types shown in the protocol documentation.
 */
public class PacketByteBuf extends ByteBuf {

    private final ByteBuf parent;
    public static final int MAX_STRING_LENGTH = 32767;

    public PacketByteBuf(ByteBuf parent) {
        this.parent = parent;
    }

    /**
     * The conversion factor to turn the protocol's 1/256th-of-a-turn unit into degrees.
     * (360 degrees / 256 units)
     */
    public static final float DEGREES_PER_PROTOCOL_ANGLE_UNIT = 360.0f / 256.0f;


    public void writeAnyNbt(Tag component) {
        if(component instanceof CompoundTag compoundTag) {
            ByteBuf fullNbtBuffer = null;
            ByteBuf strippedNbtBuffer = null;
            try {
                // 1. Serialize the tag to a full byte array.
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                new Nbt().toStream(compoundTag, new DataOutputStream(baos));
                byte[] fullNbtBytes = baos.toByteArray();

                // 2. Wrap the full NBT data in a ByteBuf.
                fullNbtBuffer = Unpooled.wrappedBuffer(fullNbtBytes);

                // 3. Strip the root name, which creates a NEW buffer.
                strippedNbtBuffer = NbtUtils.stripNbtRootName(fullNbtBuffer);

                // 4. Write the final result to the main network buffer.
                this.writeBytes(strippedNbtBuffer);

            } catch (IOException e) {
                throw new EncoderException("Failed to write TextComponent NBT", e);
            } finally {
                // 5. Release ALL temporary buffers created in the try block.
                // This block is guaranteed to execute, preventing leaks and crashes.
                if (fullNbtBuffer != null) {
                    fullNbtBuffer.release();
                }
                if (strippedNbtBuffer != null) {
                    strippedNbtBuffer.release();
                }
            }
        } else if (component instanceof StringTag stringTag) {
            writeByte(8);
            writeShort(stringTag.getValue().length());
            writeString(stringTag.getValue());
        } else {
            throw new EncoderException("Unsupported tag type for writeAnyNbt: " + component.getClass().getSimpleName());
        }

    }



    public Tag readAnyNbt() {
        try {
            ByteBuf nbt = new MiniNBT(this).parse();
            if(nbt.readableBytes() < 3 || this.getByte(this.readerIndex()) == 0x00)
                return new CompoundTag();

            if(this.getByte(this.readerIndex()) != 0x08) {
                ByteBuf correctedNbtBuffer = NbtUtils.convertNetworkNbtToNormalNbt(nbt,"root");
                DataInputStream dis = new DataInputStream(new ByteBufInputStream(correctedNbtBuffer, true));
                NbtReader reader = new NbtReader(new TagTypeRegistry());
                Logger.debug("Boutta read a " + this.getByte(this.readerIndex()) + " Not a 0x08");
                CompoundTag tag = reader.fromStream(dis);
                correctedNbtBuffer.release();
                nbt.release();
                return tag;
            } else { // NBTLIB Cant handle String as Root Tags for some reason
                this.readByte(); // 0x08
                short length = this.readShort();
                String text = this.readString(length);
                return new StringTag(text);
            }
        } catch (IOException e) {
            throw new DecoderException("Failed to read length-prefixed NBT", e);
        }
    }

    /**
     * Reads a 1-byte angle from the buffer and converts it directly to degrees.
     * This method uses a signed byte, which correctly maps the full circle for yaw.
     * <p>
     * Standard Minecraft conventions:
     * <ul>
     *   <li><b>Yaw (horizontal):</b> Ranges from -180 to +180. 0=South, -90=East, 180=North, +90=West.</li>
     *   <li><b>Pitch (vertical):</b> Ranges from -90 to +90. -90=Up, 0=Forward, +90=Down.</li>
     * </ul>
     *
     * @return The angle in degrees.
     */
    public float readAngleAsDegrees() {
        return this.readByte() * DEGREES_PER_PROTOCOL_ANGLE_UNIT;
    }







    /**
     * Converts an angle from degrees into the protocol's 1-byte format and writes it to the buffer.
     *
     * @param degrees The angle to write, in degrees. It will be scaled and written as a single byte.
     */
    public void writeAngleInDegrees(float degrees) {
        this.writeByte((byte) (degrees / DEGREES_PER_PROTOCOL_ANGLE_UNIT));
    }

    public long[] readPrefixedLongArray() {
        // 1. Read the length of the array, which is encoded as a VarInt.
        final int length = this.readVarInt();

        // 2. Perform sanity checks.
        if (length < 0) {
            throw new DecoderException("The received array length is negative: " + length);
        }

        final int requiredBytes = length * Long.BYTES; // Long.BYTES is 8
        if (readableBytes() < requiredBytes) {
            throw new DecoderException(String.format(
                    "Not enough readable bytes. Required %d, but only %d available.",
                    requiredBytes, readableBytes()
            ));
        }

        // 3. Create the long array and populate it.
        final long[] array = new long[length];
        for (int i = 0; i < length; i++) {
            array[i] = readLong();
        }

        return array;
    }

    /**
     * Writes a long array to the buffer, prefixing it with its length as a VarInt.
     * This is the corresponding encoder for readPrefixedLongArray.
     *
     * @param array The long array to serialize.
     */
    public void writePrefixedLongArray(long[] array) {
        this.writeVarInt(array.length);
        for (long value : array) {
            writeLong(value);
        }
    }

    /**
     * Reads a fixed-size bitset from the buffer and converts it into an EnumSet.
     * This is for the "EnumSet" type described in the protocol.
     *
     * @param enumClass The class of the enum whose constants will populate the set.
     *                  The enum must implement an interface that provides its mask.
     * @param <E> The enum type.
     * @return An EnumSet containing the enums corresponding to the set bits.
     */
    public <E extends Enum<E> & Maskable> EnumSet<E> readFixedBitSetEnumSet(Class<E> enumClass) {
        E[] constants = enumClass.getEnumConstants();
        int n = constants.length;
        int numBytes = (n + 7) / 8; // ceil(n / 8)
        byte[] data = this.IreadBytes(numBytes); // Read fixed number of bytes

        EnumSet<E> set = EnumSet.noneOf(enumClass);
        for (int i = 0; i < n; i++) {
            int byteIndex = i / 8;
            int bitIndex = i % 8;
            if ((data[byteIndex] & (1 << bitIndex)) != 0) {
                set.add(constants[i]);
            }
        }

        return set;
    }

    /**
     * Writes an EnumSet to the buffer as a fixed-size bitset (VarLong).
     *
     * @param set The EnumSet to write.
     * @param <E> The enum type, which must provide a mask.
     */
    public <E extends Enum<E> & Maskable> void writeEnumSet(EnumSet<E> set) {
        long bitmask = 0;
        for (E constant : set) {
            bitmask |= constant.getMask();
        }
        this.writeVarLong(bitmask);
    }

    /**
     * An interface for enums that have an associated bitmask.
     */
    public interface Maskable {
        long getMask();
    }


    // ===================================================================================
    //  PRIMITIVE DATA TYPES
    // ===================================================================================

    public int readVarInt() {
        int i = 0;
        int j = 0;
        byte b;
        do {
            b = this.readByte();
            i |= (b & 127) << j++ * 7;
            if (j > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while ((b & 128) == 128);
        return i;
    }

    public void writeVarInt(int value) {
        while ((value & -128) != 0) {
            this.writeByte(value & 127 | 128);
            value >>>= 7;
        }
        this.writeByte(value);
    }

    public long readVarLong() {
        long i = 0L;
        int j = 0;
        byte b;
        do {
            b = this.readByte();
            i |= (long) (b & 127) << j++ * 7;
            if (j > 10) {
                throw new RuntimeException("VarLong too big");
            }
        } while ((b & 128) == 128);
        return i;
    }

    public void writeVarLong(long value) {
        while ((value & -128L) != 0L) {
            this.writeByte((int) (value & 127L) | 128);
            value >>>= 7;
        }
        this.writeByte((int) value);
    }

    public String readString(int maxLength) {
        int length = readVarInt();
        if (length > maxLength * 4) {
            throw new DecoderException("The received string length is longer than maximum allowed (" + length + " > " + maxLength * 4 + ")");
        } else if (length < 0) {
            throw new DecoderException("The received string length is less than zero");
        }
        String s = toString(this.readerIndex(), length, StandardCharsets.UTF_8);
        this.readerIndex(this.readerIndex() + length);
        return s;
    }

    public String readString() {
        return readString(256); // TODO: SETTING
    }

    public void writeString(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_STRING_LENGTH) {
            throw new EncoderException("String too big (was " + bytes.length + " bytes encoded, max " + MAX_STRING_LENGTH + ")");
        }
        writeVarInt(bytes.length);
        this.writeBytes(bytes);
    }

    public String readIdentifierString() {
        return this.readString(MAX_STRING_LENGTH);
    }


    public UUID readUuid() {
        return new UUID(this.readLong(), this.readLong());
    }

    public void writeUuid(UUID uuid) {
        this.writeLong(uuid.getMostSignificantBits());
        this.writeLong(uuid.getLeastSignificantBits());
    }

    public float readAngle() {
        return this.readUnsignedByte() / 256.0f;
    }

    public void writeAngle(float angle) {
        this.writeByte((int) (angle * 256.0f));
    }


    // ===================================================================================
    //  COMPLEX & GENERIC DATA TYPES
    // ===================================================================================

    public byte[] readByteArray(int maxLength) {
        int i = this.readVarInt();
        if (i > maxLength) {
            throw new DecoderException("ByteArray with size " + i + " is bigger than allowed " + maxLength);
        }
        byte[] bs = new byte[i];
        this.readBytes(bs);
        return bs;
    }

    public byte[] IreadBytes(int maxLength) {
        byte[] bs = new byte[maxLength];
        this.readBytes(bs);
        return bs;
    }

    public void writeByteArray(byte[] array) {
        this.writeVarInt(array.length);
        this.writeBytes(array);
    }

    public <T> List<T> readPrefixedArray(Function<PacketByteBuf, T> reader) {
        int size = readVarInt();
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            list.add(reader.apply(this));
        }
        return list;
    }




    public byte[] readPrefixedByteArray() {
        int size = readVarInt();
        return readByteArray(size);
    }

    public <T> List<T> readArray(Function<PacketByteBuf, T> reader, int size) {
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            list.add(reader.apply(this));
        }
        return list;
    }

    public <T> void writePrefixedArray(Collection<T> collection, BiConsumer<PacketByteBuf, T> writer) {
        writeVarInt(collection.size());
        for (T item : collection) {
            writer.accept(this, item);
        }
    }

    public void writePrefixedByteArray(byte[] array) {
        writeVarInt(array.length);
        this.writeBytes(array);
    }

    public <T> Optional<T> readOptional(Function<PacketByteBuf, T> reader) {
        if (this.readBoolean()) {
            return Optional.of(reader.apply(this));
        }
        return Optional.empty();
    }

    public <T> void writeOptional(Optional<T> optional, BiConsumer<PacketByteBuf, T> writer) {
        this.writeBoolean(optional.isPresent());
        optional.ifPresent(value -> writer.accept(this, value));
    }

    public BitSet readBitSet() {
        return BitSet.valueOf(this.readPrefixedArray(PacketByteBuf::readLong).stream().mapToLong(l -> l).toArray());
    }

    public void writeBitSet(BitSet bitSet) {
        this.writePrefixedArray(Arrays.stream(bitSet.toLongArray()).boxed().toList(), PacketByteBuf::writeLong);
    }
    public BitSet readFixedBitSet(int size) {
        int bytes = (size + 7) >> 3; // ceil(size / 8)
        byte[] data = new byte[bytes];
        for (int i = 0; i < bytes; i++) {
            data[i] = this.readByte();
        }

        BitSet bitSet = new BitSet(size);
        for (int i = 0; i < size; i++) {
            if ((data[i / 8] & (1 << (i % 8))) != 0) {
                bitSet.set(i);
            }
        }
        return bitSet;
    }

    public void writeFixedBitSet(BitSet bitSet, int size) {
        int bytes = (size + 7) >> 3;
        byte[] data = new byte[bytes];

        for (int i = 0; i < size; i++) {
            if (bitSet.get(i)) {
                data[i / 8] |= 1 << (i % 8);
            }
        }

        for (byte b : data) {
            this.writeByte(b);
        }
        Logger.nul("");
    }

    public List<String> readStringList() {
        return readPrefixedArray(buf -> buf.readString(MAX_STRING_LENGTH));
    }

    public void writeStringCollection(Collection<String> strings) {
        writePrefixedArray(strings, PacketByteBuf::writeString);
    }



    // --- DELEGATED METHODS REQUIRED BY ABSTRACT ByteBuf ---

    @Override public int capacity() { return parent.capacity(); }
    @Override public ByteBuf capacity(int newCapacity) { parent.capacity(newCapacity); return this; }
    @Override public int maxCapacity() { return parent.maxCapacity(); }
    @Override public ByteBufAllocator alloc() { return parent.alloc(); }
    @Override public ByteOrder order() { return parent.order(); }
    @Override public ByteBuf order(ByteOrder endianness) { return parent.order(endianness); }
    @Override public ByteBuf unwrap() { return parent; }
    @Override public boolean isDirect() { return parent.isDirect(); }
    @Override public boolean isReadOnly() { return parent.isReadOnly(); }
    @Override public ByteBuf asReadOnly() { return parent.asReadOnly(); }
    @Override public int readerIndex() { return parent.readerIndex(); }
    @Override public ByteBuf readerIndex(int readerIndex) { parent.readerIndex(readerIndex); return this; }
    @Override public int writerIndex() { return parent.writerIndex(); }
    @Override public ByteBuf writerIndex(int writerIndex) { parent.writerIndex(writerIndex); return this; }
    @Override public ByteBuf setIndex(int readerIndex, int writerIndex) { parent.setIndex(readerIndex, writerIndex); return this; }
    @Override public int readableBytes() { return parent.readableBytes(); }
    @Override public int writableBytes() { return parent.writableBytes(); }
    @Override public int maxWritableBytes() { return parent.maxWritableBytes(); }
    @Override public boolean isReadable() { return parent.isReadable(); }
    @Override public boolean isReadable(int size) { return parent.isReadable(size); }
    @Override public boolean isWritable() { return parent.isWritable(); }
    @Override public boolean isWritable(int size) { return parent.isWritable(size); }
    @Override public ByteBuf clear() { parent.clear(); return this; }
    @Override public ByteBuf markReaderIndex() { parent.markReaderIndex(); return this; }
    @Override public ByteBuf resetReaderIndex() { parent.resetReaderIndex(); return this; }
    @Override public ByteBuf markWriterIndex() { parent.markWriterIndex(); return this; }
    @Override public ByteBuf resetWriterIndex() { parent.resetWriterIndex(); return this; }
    @Override public ByteBuf discardReadBytes() { parent.discardReadBytes(); return this; }
    @Override public ByteBuf discardSomeReadBytes() { parent.discardSomeReadBytes(); return this; }
    @Override public ByteBuf ensureWritable(int minWritableBytes) { parent.ensureWritable(minWritableBytes); return this; }
    @Override public int ensureWritable(int minWritableBytes, boolean force) { return parent.ensureWritable(minWritableBytes, force); }
    @Override public boolean getBoolean(int index) { return parent.getBoolean(index); }
    @Override public byte getByte(int index) { return parent.getByte(index); }
    @Override public short getUnsignedByte(int index) { return parent.getUnsignedByte(index); }
    @Override public short getShort(int index) { return parent.getShort(index); }
    @Override public short getShortLE(int index) { return parent.getShortLE(index); }
    @Override public int getUnsignedShort(int index) { return parent.getUnsignedShort(index); }
    @Override public int getUnsignedShortLE(int index) { return parent.getUnsignedShortLE(index); }
    @Override public int getMedium(int index) { return parent.getMedium(index); }
    @Override public int getMediumLE(int index) { return parent.getMediumLE(index); }
    @Override public int getUnsignedMedium(int index) { return parent.getUnsignedMedium(index); }
    @Override public int getUnsignedMediumLE(int index) { return parent.getUnsignedMediumLE(index); }
    @Override public int getInt(int index) { return parent.getInt(index); }
    @Override public int getIntLE(int index) { return parent.getIntLE(index); }
    @Override public long getUnsignedInt(int index) { return parent.getUnsignedInt(index); }
    @Override public long getUnsignedIntLE(int index) { return parent.getUnsignedIntLE(index); }
    @Override public long getLong(int index) { return parent.getLong(index); }
    @Override public long getLongLE(int index) { return parent.getLongLE(index); }
    @Override public char getChar(int index) { return parent.getChar(index); }
    @Override public float getFloat(int index) { return parent.getFloat(index); }
    @Override public double getDouble(int index) { return parent.getDouble(index); }
    @Override public ByteBuf getBytes(int index, ByteBuf dst) { parent.getBytes(index, dst); return this; }
    @Override public ByteBuf getBytes(int index, ByteBuf dst, int length) { parent.getBytes(index, dst, length); return this; }
    @Override public ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length) { parent.getBytes(index, dst, dstIndex, length); return this; }
    @Override public ByteBuf getBytes(int index, byte[] dst) { parent.getBytes(index, dst); return this; }
    @Override public ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length) { parent.getBytes(index, dst, dstIndex, length); return this; }
    @Override public ByteBuf getBytes(int index, ByteBuffer dst) { parent.getBytes(index, dst); return this; }
    @Override public ByteBuf getBytes(int index, OutputStream out, int length) throws IOException { parent.getBytes(index, out, length); return this; }
    @Override public int getBytes(int index, GatheringByteChannel out, int length) throws IOException { return parent.getBytes(index, out, length); }
    @Override public int getBytes(int index, FileChannel out, long position, int length) throws IOException { return parent.getBytes(index, out, position, length); }
    @Override public CharSequence getCharSequence(int index, int length, Charset charset) { return parent.getCharSequence(index, length, charset); }
    @Override public ByteBuf setBoolean(int index, boolean value) { parent.setBoolean(index, value); return this; }
    @Override public ByteBuf setByte(int index, int value) { parent.setByte(index, value); return this; }
    @Override public ByteBuf setShort(int index, int value) { parent.setShort(index, value); return this; }
    @Override public ByteBuf setShortLE(int index, int value) { parent.setShortLE(index, value); return this; }
    @Override public ByteBuf setMedium(int index, int value) { parent.setMedium(index, value); return this; }
    @Override public ByteBuf setMediumLE(int index, int value) { parent.setMediumLE(index, value); return this; }
    @Override public ByteBuf setInt(int index, int value) { parent.setInt(index, value); return this; }
    @Override public ByteBuf setIntLE(int index, int value) { parent.setIntLE(index, value); return this; }
    @Override public ByteBuf setLong(int index, long value) { parent.setLong(index, value); return this; }
    @Override public ByteBuf setLongLE(int index, long value) { parent.setLongLE(index, value); return this; }
    @Override public ByteBuf setChar(int index, int value) { parent.setChar(index, value); return this; }
    @Override public ByteBuf setFloat(int index, float value) { parent.setFloat(index, value); return this; }
    @Override public ByteBuf setDouble(int index, double value) { parent.setDouble(index, value); return this; }
    @Override public ByteBuf setBytes(int index, ByteBuf src) { parent.setBytes(index, src); return this; }
    @Override public ByteBuf setBytes(int index, ByteBuf src, int length) { parent.setBytes(index, src, length); return this; }
    @Override public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length) { parent.setBytes(index, src, srcIndex, length); return this; }
    @Override public ByteBuf setBytes(int index, byte[] src) { parent.setBytes(index, src); return this; }
    @Override public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length) { parent.setBytes(index, src, srcIndex, length); return this; }
    @Override public ByteBuf setBytes(int index, ByteBuffer src) { parent.setBytes(index, src); return this; }
    @Override public int setBytes(int index, InputStream in, int length) throws IOException { return parent.setBytes(index, in, length); }
    @Override public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException { return parent.setBytes(index, in, length); }
    @Override public int setBytes(int index, FileChannel in, long position, int length) throws IOException { return parent.setBytes(index, in, position, length); }
    @Override public ByteBuf setZero(int index, int length) { parent.setZero(index, length); return this; }
    @Override public int setCharSequence(int index, CharSequence sequence, Charset charset) { return parent.setCharSequence(index, sequence, charset); }
    @Override public boolean readBoolean() { return parent.readBoolean(); }
    @Override public byte readByte() { return parent.readByte(); }
    @Override public short readUnsignedByte() { return parent.readUnsignedByte(); }
    @Override public short readShort() { return parent.readShort(); }
    @Override public short readShortLE() { return parent.readShortLE(); }
    @Override public int readUnsignedShort() { return parent.readUnsignedShort(); }
    @Override public int readUnsignedShortLE() { return parent.readUnsignedShortLE(); }
    @Override public int readMedium() { return parent.readMedium(); }
    @Override public int readMediumLE() { return parent.readMediumLE(); }
    @Override public int readUnsignedMedium() { return parent.readUnsignedMedium(); }
    @Override public int readUnsignedMediumLE() { return parent.readUnsignedMediumLE(); }
    @Override public int readInt() { return parent.readInt(); }
    @Override public int readIntLE() { return parent.readIntLE(); }
    @Override public long readUnsignedInt() { return parent.readUnsignedInt(); }
    @Override public long readUnsignedIntLE() { return parent.readUnsignedIntLE(); }
    @Override public long readLong() { return parent.readLong(); }
    @Override public long readLongLE() { return parent.readLongLE(); }
    @Override public char readChar() { return parent.readChar(); }
    @Override public float readFloat() { return parent.readFloat(); }
    @Override public double readDouble() { return parent.readDouble(); }
    @Override public ByteBuf readBytes(int length) { return parent.readBytes(length); }
    @Override public ByteBuf readSlice(int length) { return parent.readSlice(length); }
    @Override public ByteBuf readRetainedSlice(int length) { return parent.readRetainedSlice(length); }
    @Override public ByteBuf readBytes(ByteBuf dst) { parent.readBytes(dst); return this; }
    @Override public ByteBuf readBytes(ByteBuf dst, int length) { parent.readBytes(dst, length); return this; }
    @Override public ByteBuf readBytes(ByteBuf dst, int dstIndex, int length) { parent.readBytes(dst, dstIndex, length); return this; }
    @Override public ByteBuf readBytes(byte[] dst) { parent.readBytes(dst); return this; }
    @Override public ByteBuf readBytes(byte[] dst, int dstIndex, int length) { parent.readBytes(dst, dstIndex, length); return this; }
    @Override public ByteBuf readBytes(ByteBuffer dst) { parent.readBytes(dst); return this; }
    @Override public ByteBuf readBytes(OutputStream out, int length) throws IOException { parent.readBytes(out, length); return this; }
    @Override public int readBytes(GatheringByteChannel out, int length) throws IOException { return parent.readBytes(out, length); }
    @Override public CharSequence readCharSequence(int length, Charset charset) { return parent.readCharSequence(length, charset); }
    @Override public int readBytes(FileChannel out, long position, int length) throws IOException { return parent.readBytes(out, position, length); }
    @Override public ByteBuf skipBytes(int length) { parent.skipBytes(length); return this; }
    @Override public ByteBuf writeBoolean(boolean value) { parent.writeBoolean(value); return this; }
    @Override public ByteBuf writeByte(int value) { parent.writeByte(value); return this; }
    @Override public ByteBuf writeShort(int value) { parent.writeShort(value); return this; }
    @Override public ByteBuf writeShortLE(int value) { parent.writeShortLE(value); return this; }
    @Override public ByteBuf writeMedium(int value) { parent.writeMedium(value); return this; }
    @Override public ByteBuf writeMediumLE(int value) { parent.writeMediumLE(value); return this; }
    @Override public ByteBuf writeInt(int value) { parent.writeInt(value); return this; }
    @Override public ByteBuf writeIntLE(int value) { parent.writeIntLE(value); return this; }
    @Override public ByteBuf writeLong(long value) { parent.writeLong(value); return this; }
    @Override public ByteBuf writeLongLE(long value) { parent.writeLongLE(value); return this; }
    @Override public ByteBuf writeChar(int value) { parent.writeChar(value); return this; }
    @Override public ByteBuf writeFloat(float value) { parent.writeFloat(value); return this; }
    @Override public ByteBuf writeDouble(double value) { parent.writeDouble(value); return this; }
    @Override public ByteBuf writeBytes(ByteBuf src) { parent.writeBytes(src); return this; }
    @Override public ByteBuf writeBytes(ByteBuf src, int length) { parent.writeBytes(src, length); return this; }
    @Override public ByteBuf writeBytes(ByteBuf src, int srcIndex, int length) { parent.writeBytes(src, srcIndex, length); return this; }
    @Override public ByteBuf writeBytes(byte[] src) { parent.writeBytes(src); return this; }
    @Override public ByteBuf writeBytes(byte[] src, int srcIndex, int length) { parent.writeBytes(src, srcIndex, length); return this; }
    @Override public ByteBuf writeBytes(ByteBuffer src) { parent.writeBytes(src); return this; }
    @Override public int writeBytes(InputStream in, int length) throws IOException { return parent.writeBytes(in, length); }
    @Override public int writeBytes(ScatteringByteChannel in, int length) throws IOException { return parent.writeBytes(in, length); }
    @Override public int writeBytes(FileChannel in, long position, int length) throws IOException { return parent.writeBytes(in, position, length); }
    @Override public ByteBuf writeZero(int length) { parent.writeZero(length); return this; }
    @Override public int writeCharSequence(CharSequence sequence, Charset charset) { return parent.writeCharSequence(sequence, charset); }
    @Override public int indexOf(int fromIndex, int toIndex, byte value) { return parent.indexOf(fromIndex, toIndex, value); }
    @Override public int bytesBefore(byte value) { return parent.bytesBefore(value); }
    @Override public int bytesBefore(int length, byte value) { return parent.bytesBefore(length, value); }
    @Override public int bytesBefore(int index, int length, byte value) { return parent.bytesBefore(index, length, value); }
    @Override public int forEachByte(ByteProcessor processor) { return parent.forEachByte(processor); }
    @Override public int forEachByte(int index, int length, ByteProcessor processor) { return parent.forEachByte(index, length, processor); }
    @Override public int forEachByteDesc(ByteProcessor processor) { return parent.forEachByteDesc(processor); }
    @Override public int forEachByteDesc(int index, int length, ByteProcessor processor) { return parent.forEachByteDesc(index, length, processor); }
    @Override public ByteBuf copy() { return parent.copy(); }
    @Override public ByteBuf copy(int index, int length) { return parent.copy(index, length); }
    @Override public ByteBuf slice() { return parent.slice(); }
    @Override public ByteBuf retainedSlice() { return parent.retainedSlice(); }
    @Override public ByteBuf slice(int index, int length) { return parent.slice(index, length); }
    @Override public ByteBuf retainedSlice(int index, int length) { return parent.retainedSlice(index, length); }
    @Override public ByteBuf duplicate() { return parent.duplicate(); }
    @Override public ByteBuf retainedDuplicate() { return parent.retainedDuplicate(); }
    @Override public int nioBufferCount() { return parent.nioBufferCount(); }
    @Override public ByteBuffer nioBuffer() { return parent.nioBuffer(); }
    @Override public ByteBuffer nioBuffer(int index, int length) { return parent.nioBuffer(index, length); }
    @Override public ByteBuffer internalNioBuffer(int index, int length) { return parent.internalNioBuffer(index, length); }
    @Override public ByteBuffer[] nioBuffers() { return parent.nioBuffers(); }
    @Override public ByteBuffer[] nioBuffers(int index, int length) { return parent.nioBuffers(index, length); }
    @Override public boolean hasArray() { return parent.hasArray(); }
    @Override public byte[] array() { return parent.array(); }
    @Override public int arrayOffset() { return parent.arrayOffset(); }
    @Override public boolean hasMemoryAddress() { return parent.hasMemoryAddress(); }
    @Override public long memoryAddress() { return parent.memoryAddress(); }
    @Override public String toString(Charset charset) { return parent.toString(charset); }
    @Override public String toString(int index, int length, Charset charset) { return parent.toString(index, length, charset); }
    @Override public int hashCode() { return parent.hashCode(); }
    @Override public boolean equals(Object obj) { return parent.equals(obj); }
    @Override public int compareTo(ByteBuf buffer) { return parent.compareTo(buffer); }
    @Override public String toString() { return parent.toString(); }
    @Override public ByteBuf retain(int increment) { parent.retain(increment); return this; }
    @Override public ByteBuf retain() { parent.retain(); return this; }
    @Override public ByteBuf touch() { parent.touch(); return this; }
    @Override public ByteBuf touch(Object hint) { parent.touch(hint); return this; }
    @Override public int refCnt() { return parent.refCnt(); }
    @Override public boolean release() { return parent.release(); }
    @Override public boolean release(int decrement) { return parent.release(decrement); }
}