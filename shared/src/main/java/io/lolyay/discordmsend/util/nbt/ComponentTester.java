package io.lolyay.discordmsend.util.nbt;

import dev.dewy.nbt.api.Tag;
import dev.dewy.nbt.tags.array.ByteArrayTag;
import dev.dewy.nbt.tags.array.IntArrayTag;
import dev.dewy.nbt.tags.array.LongArrayTag;
import dev.dewy.nbt.tags.collection.CompoundTag;
import dev.dewy.nbt.tags.collection.ListTag;

import java.util.List;
import java.util.Objects;

public class ComponentTester {

    /**
     * Returns true if all tags in {@code expected} are present and equal (recursively)
     * in {@code actual}, following Minecraft's "partial" NBT match rules.
     */
    public static boolean matches(CompoundTag expected, CompoundTag actual) {
        for (String key : expected.getValue().keySet()) {
            if (!actual.getValue().containsKey(key))
                return false;

            Tag expectedTag = expected.get(key);
            Tag actualTag = actual.get(key);

            if (!compareTags(expectedTag, actualTag))
                return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static boolean compareTags(Tag expected, Tag actual) {
        // Mismatched tag types fail unless both are compounds/lists handled separately
        if (expected instanceof CompoundTag expectedCompound && actual instanceof CompoundTag actualCompound)
            return matches(expectedCompound, actualCompound);

        if (expected instanceof ListTag expectedList && actual instanceof ListTag actualList)
            return compareLists(expectedList, actualList);

        if (expected instanceof ByteArrayTag || expected instanceof IntArrayTag || expected instanceof LongArrayTag)
            return Objects.equals(expected, actual); // strict

        // For all primitives, tag type must match exactly and value equal
        if (!expected.getClass().equals(actual.getClass()))
            return false;

        return Objects.equals(expected.getValue(), actual.getValue());
    }

    /**
     * Partial list comparison — each element in expected must be contained in actual,
     * order and count ignored. Empty lists match only empty lists.
     */
    private static boolean compareLists(ListTag<? extends Tag> expected, ListTag<? extends Tag> actual) {
        List<Tag> expectedValues = (List<Tag>) expected.getValue();
        List<Tag> actualValues = (List<Tag>) actual.getValue();

        if (expectedValues.isEmpty())
            return actualValues.isEmpty();

        // For each expected element, at least one actual element must match
        for (Tag expectedElement : expectedValues) {
            boolean matched = false;
            for (Tag actualElement : actualValues) {
                if (compareTags(expectedElement, actualElement)) {
                    matched = true;
                    break;
                }
            }
            if (!matched)
                return false;
        }
        return true;
    }
}
