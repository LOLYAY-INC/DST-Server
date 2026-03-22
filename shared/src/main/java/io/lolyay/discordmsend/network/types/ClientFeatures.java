package io.lolyay.discordmsend.network.types;

import java.util.BitSet;
import java.util.Objects;


public class ClientFeatures {
    private static final int MAX_FEATURES = 8;
    private final BitSet features;


    public ClientFeatures() {
        this.features = new BitSet(MAX_FEATURES);
    }


    public boolean contains(Feature feature){
        return features.get(feature.ordinal());
    }


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

    public ClientFeatures with(Feature feature) {
        this.enable(feature);
        return this;
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

    public enum Feature {
        IS_DISCORD_BOT,
        FORCE_OPUS,
        DISABLE_KEEP_ALIVE, //UNUSED
        UDP_ME_PLZ,
        WANTS_E2EE, //UNUSED
        ALLOW_LIST_IN_ACTIVE_CONNECTIONS, //UNUSED
        SUPPORTS_SEEKING, //UNUSED
        USES_MEDIA //UNUSED
    }
}
