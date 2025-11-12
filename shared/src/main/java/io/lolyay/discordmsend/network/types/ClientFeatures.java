package io.lolyay.discordmsend.network.types;

import java.util.BitSet;
import java.util.Objects;

/**
 * Represents a set of client features that can be enabled or disabled.
 * <p/>
 * Description of all ClientFeatures:
 * <p/>
 * <p/>
 * <p> {@link ClientFeatures.Feature#IS_DISCORD_BOT}: If you dont provide this flag, the server will send all music data via opus to you directly, and you cant use features like connecting to discord! see {@link io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.AudioS2CPacket} </p>
 * <p/>
 * <p> {@link ClientFeatures.Feature#USES_OPUS}: Indicates that the client can receive and decode raw Opus-encoded audio packets. When enabled, the server streams music data in Opus format instead of decoded PCM. Required for low-latency audio delivery and mandatory if {@code IS_DISCORD_BOT} is {@code false}. </p>
 * <p/>
 * <p> {@link ClientFeatures.Feature#DISABLE_KEEP_ALIVE}: Instructs the server to skip sending periodic keep-alive packets. Useful for clients on extremely constrained networks or when implementing custom ping/pong logic. Note that disabling keep-alives may lead to earlier connection timeouts if the underlying TCP socket becomes idle.</p>
 * <p/>
 * <p> {@link ClientFeatures.Feature#UDP_ME_PLZ}: This does NOT DO WHAT IT SEEMS! If this is enabled and {@link ClientFeatures.Feature#IS_DISCORD_BOT} is disabled, the server will send all music data as fast as possible! If it is disabled, you will receive each Opus frame at a 20 ms interval!</p>
 * <p/>
 * <p> {@link ClientFeatures.Feature#WANTS_E2EE}: Signals that the client wishes to enable end-to-end encryption for discord Voice Chat. This will become mandatory sometime in 2026.</p>
 * <p/>
 * <p> {@link ClientFeatures.Feature#ALLOW_LIST_IN_ACTIVE_CONNECTIONS}: Permits the server to list you in the Statistics with Name, Author, Version and Features.</p>
 * <p/>
 * <p> {@link ClientFeatures.Feature#SUPPORTS_SEEKING}: Declares that the client can handle seek requests and timeline repositioning. This will become possible in the future, and if you want the best audio Quality, turn this off.</p>
 * <p/>
 * <p> {@link ClientFeatures.Feature#USES_MEDIA}: This is currently Unused</p>
 *
 * @apiNote It is really Important to actually tell the server what you support, else you won't experience the functionality that you want!!!
 */
public class ClientFeatures {
    private static final int MAX_FEATURES = 8;
    private final BitSet features;

    /**
     * Creates a new ClientFeatures instance with all features disabled.
     */
    public ClientFeatures() {
        this.features = new BitSet(MAX_FEATURES);
    }


    public boolean contains(Feature feature){
        return features.get(feature.ordinal());
    }
    /**
     * Creates a new ClientFeatures instance from a byte value.
     *
     * @param value The byte value representing the feature flags
     */
    public ClientFeatures(byte value) {
        this();
        setFromByte(value);
    }

    public ClientFeatures(Feature... features){
        this();
        for (Feature feature : features) {
            enable(feature);
        }
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
     * Enables the specified feature and returns the modified ClientFeatures instance.
     *
     * @param feature The feature to enable
     * @return The modified ClientFeatures instance
     * @throws IndexOutOfBoundsException if the feature index is >= 8
     */
    public ClientFeatures with(Feature feature) {
        this.enable(feature);
        return this;
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
        ClientFeatures that = (ClientFeatures) o;
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
            sb.append(features.get(i) ? Feature.values()[i] : "").append(", ");
        }
        return sb.append('}').toString();
    }

    /**
     * Enum representing available features.
     *
     */
    public enum Feature {
        IS_DISCORD_BOT,
        USES_OPUS,
        DISABLE_KEEP_ALIVE,
        UDP_ME_PLZ,
        WANTS_E2EE,
        ALLOW_LIST_IN_ACTIVE_CONNECTIONS,
        SUPPORTS_SEEKING,
        USES_MEDIA
    }
}
