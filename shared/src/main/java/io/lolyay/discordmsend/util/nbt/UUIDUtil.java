package io.lolyay.discordmsend.util.nbt;

import dev.dewy.nbt.api.Tag;
import dev.dewy.nbt.tags.array.IntArrayTag;
import dev.dewy.nbt.tags.collection.ListTag;
import dev.dewy.nbt.tags.primitive.IntTag;
import dev.dewy.nbt.tags.primitive.StringTag;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

public class UUIDUtil {
    public static UUID parseUuid(Tag tag) {
        // Case 1: The UUID is a standard String.
        if (tag instanceof StringTag) {
            return UUID.fromString(((StringTag) tag).getValue());
        }

        // Case 2: The UUID is an Int Array [I; 1, 2, 3, 4].
        if (tag instanceof IntArrayTag) {
            int[] intArray = ((IntArrayTag) tag).getValue();
            if (intArray.length != 4) {
                throw new IllegalArgumentException("IntArrayTag must contain exactly 4 integers to be parsed as a UUID.");
            }
            return uuidFromIntArray(intArray);
        }

        // Case 3: The UUID is a List of Ints [1, 2, 3, 4].
        if (tag instanceof ListTag) {
            ListTag<IntTag> listTag = (ListTag<IntTag>) tag;
            // Ensure it's a list of IntTags and has the correct size.
            int[] intArray = new int[4];
            for (int i = 0; i < 4; i++) {
                intArray[i] = listTag.get(i).getValue();
            }
            return uuidFromIntArray(intArray);

        }

        // If the tag is none of the above types, it's an error.
        throw new IllegalArgumentException("Tag must be a StringTag, IntArrayTag, or ListTag of Ints to be parsed as a UUID, but was " + tag.getClass().getSimpleName());
    }

    /**
     * Converts a UUID into an array of four integers.
     * This is the inverse operation of creating a UUID from an int array.
     *
     * @param uuid The UUID to convert.
     * @return An int array of size 4 representing the UUID.
     */
    public static List<Integer> toIntArray(UUID uuid) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());
        byteBuffer.flip();

        List<Integer> result = new java.util.ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            result.add(byteBuffer.getInt());
        }
        return result;
    }
    public static String toUndashedString(UUID uuid) {
        return uuid.toString().replace("-", "");
    }

    /**
     * Creates a UUID object from its 32-character undashed string representation.
     * e.g., 853c80ef3c3749fdaa49938b674adae6 -> 853c80ef-3c37-49fd-aa49-938b674adae6
     *
     * @param undashedUuid The 32-character hexadecimal string.
     * @return The corresponding UUID object.
     * @throws IllegalArgumentException if the string is not 32 characters long.
     */
    public static UUID fromUndashedString(String undashedUuid) {
        if (undashedUuid == null || undashedUuid.length() != 32) {
            throw new IllegalArgumentException("Undashed UUID must be 32 characters long.");
        }
        String dashedUuid = undashedUuid.substring(0, 8) + "-" +
                undashedUuid.substring(8, 12) + "-" +
                undashedUuid.substring(12, 16) + "-" +
                undashedUuid.substring(16, 20) + "-" +
                undashedUuid.substring(20, 32);
        return UUID.fromString(dashedUuid);
    }

    /**
     * Creates a UUID from an array of four integers.
     * The first two integers form the most significant long, and the last two
     * form the least significant long.
     */
    private static UUID uuidFromIntArray(int[] array) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
        for (int i : array) {
            byteBuffer.putInt(i);
        }
        byteBuffer.flip();
        return new UUID(byteBuffer.getLong(), byteBuffer.getLong());
    }
}