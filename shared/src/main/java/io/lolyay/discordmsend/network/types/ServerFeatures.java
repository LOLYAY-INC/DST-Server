package io.lolyay.discordmsend.network.types;

import java.util.BitSet;
import java.util.Objects;


public class ServerFeatures {
    private static final int MAX_FEATURES = 8;
    private final BitSet features;


    public ServerFeatures() {
        this.features = new BitSet(MAX_FEATURES);
    }

    public ServerFeatures(Feature... features){
        this();
        for (Feature feature : features) {
            enable(feature);
        }
    }

    public ServerFeatures(byte value) {
        this();
        setFromByte(value);
    }

    public void enable(Feature feature) {
        checkFeatureIndex(feature.ordinal());
        features.set(feature.ordinal(), true);
    }

    public void disable(Feature feature) {
        checkFeatureIndex(feature.ordinal());
        features.set(feature.ordinal(), false);
    }

    public boolean toggle(Feature feature) {
        checkFeatureIndex(feature.ordinal());
        features.flip(feature.ordinal());
        return isEnabled(feature);
    }

    public boolean isEnabled(Feature feature) {
        checkFeatureIndex(feature.ordinal());
        return features.get(feature.ordinal());
    }

    public byte toByte() {
        if (features.isEmpty()) {
            return 0;
        }
        byte[] bytes = features.toByteArray();
        return bytes.length > 0 ? bytes[0] : 0;
    }

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
        StringBuilder sb = new StringBuilder("ServerFeatures{");
        for (int i = 0; i < MAX_FEATURES; i++) {
            if (i > 0) sb.append(", ");
            sb.append(i).append("=").append(features.get(i));
        }
        return sb.append('}').toString();
    }


    public enum Feature {
        IS_DISCORD_ALLOWED, //UNUSED
        CAN_OPUS, //UNUSED
        DOES_KEEP_ALIVE, //UNUSED
        UNUSED, //UNUSED
        CAN_E2EE, //UNUSED
        CAN_DO_YOUTUBE, //UNUSED
        SUPPORTS_SEEKING, //UNUSED
        USES_MEDIA //UNUSED
    }
}
