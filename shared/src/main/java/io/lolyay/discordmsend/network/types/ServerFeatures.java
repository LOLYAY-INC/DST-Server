package io.lolyay.discordmsend.network.types;

import java.util.BitSet;
import java.util.Objects;

/**
 * Represents a set of client features that can be enabled or disabled.
 * Uses a BitSet for efficient storage and manipulation of feature flags.
 * Supports up to 8 features (1 byte).
 */
public class ServerFeatures {
    private static final int MAX_FEATURES = 8;
    private final BitSet features;

    /**
     * Creates a new ClientFeatures instance with all features disabled.
     */
    public ServerFeatures() {
        this.features = new BitSet(MAX_FEATURES);
    }

    public ServerFeatures(Feature... features){
        this();
        for (Feature feature : features) {
            enable(feature);
        }
    }

    /**
     * Creates a new ClientFeatures instance from a byte value.
     *
     * @param value The byte value representing the feature flags
     */
    public ServerFeatures(byte value) {
        this();
        setFromByte(value);
    }

    /**
     * Enables the specified feature.
     *
     * @param feature The feature to enable
     * @throws IndexOutOfBoundsException if the feature index is >= 8
     */
    public void enable(Feature feature) {
        checkFeatureIndex(feature.ordinal());
        features.set(feature.ordinal(), true);
    }

    /**
     * Disables the specified feature.
     *
     * @param feature The feature to disable
     * @throws IndexOutOfBoundsException if the feature index is >= 8
     */
    public void disable(Feature feature) {
        checkFeatureIndex(feature.ordinal());
        features.set(feature.ordinal(), false);
    }

    /**
     * Toggles the specified feature.
     *
     * @param feature The feature to toggle
     * @return The new state of the feature
     * @throws IndexOutOfBoundsException if the feature index is >= 8
     */
    public boolean toggle(Feature feature) {
        checkFeatureIndex(feature.ordinal());
        features.flip(feature.ordinal());
        return isEnabled(feature);
    }

    /**
     * Checks if a feature is enabled.
     *
     * @param feature The feature to check
     * @return true if the feature is enabled, false otherwise
     * @throws IndexOutOfBoundsException if the feature index is >= 8
     */
    public boolean isEnabled(Feature feature) {
        checkFeatureIndex(feature.ordinal());
        return features.get(feature.ordinal());
    }

    /**
     * Returns the features as a byte value.
     *
     * @return A byte value representing the enabled features
     */
    public byte toByte() {
        if (features.isEmpty()) {
            return 0;
        }
        byte[] bytes = features.toByteArray();
        return bytes.length > 0 ? bytes[0] : 0;
    }

    /**
     * Sets the features from a byte value.
     *
     * @param value The byte value representing the feature flags
     */
    public void setFromByte(byte value) {
        features.clear();
        if (value != 0) {
            for (int i = 0; i < MAX_FEATURES; i++) {
                if ((value & (1 << i)) != 0) {
                    features.set(i);
                }
            }
        }
    }

    private void checkFeatureIndex(int index) {
        if (index >= MAX_FEATURES) {
            throw new IndexOutOfBoundsException("Feature index " + index + " exceeds maximum of " + (MAX_FEATURES - 1));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerFeatures that = (ServerFeatures) o;
        return Objects.equals(features, that.features);
    }

    @Override
    public int hashCode() {
        return Objects.hash(features);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ClientFeatures{");
        for (int i = 0; i < MAX_FEATURES; i++) {
            if (i > 0) sb.append(", ");
            sb.append(i).append("=").append(features.get(i));
        }
        return sb.append('}').toString();
    }

    /**
     * Enum representing available features.
     * Maximum of 8 features are supported (0-7).
     */
    public enum Feature {
        IS_DISCORD_ALLOWED,
        CAN_OPUS,
        DOES_KEEP_ALIVE,
        USES_CUSTOM_CIPHER_SERVER,
        CAN_E2EE,
        CAN_DO_YOUTUBE,
        SUPPORTS_SEEKING,
        USES_MEDIA
    }
}
