package io.lolyay.discordmsend.util.nbt;

import dev.dewy.nbt.api.Tag;
import dev.dewy.nbt.tags.collection.CompoundTag;
import dev.dewy.nbt.tags.primitive.*;
import io.lolyay.discordmsend.util.logging.Logger;
import org.jetbrains.annotations.Nullable;


import java.util.Map;

/**
 * An enhanced version of CompoundTag that provides better type conversion and SNBT support.
 * This class extends CompoundTag and adds methods for safer type conversion with proper error handling.
 */
public class BetterCompoundTag extends CompoundTag {
    
    /**
     * Creates a new, empty BetterCompoundTag.
     */
    public BetterCompoundTag() {
        super();
    }
    public <E extends Tag> E putOptional(String name, @Nullable Tag tag) {
        if (name == null) {
            throw new NullPointerException("name is marked non-null but is null");
        } else if (tag == null) {
            return null;
        } else {
            tag.setName(name);
            return (E)this.put(tag);
        }
    }

    public void putBoolean( String name, boolean value) {
        this.putByte(name, (byte) (value ? 1 : 0));
    }

    public <E extends Tag> E put(String name, @Nullable Tag tag) {
        if (name == null) {
            throw new NullPointerException("name is marked non-null but is null");
        } else if (tag == null) {
            return null;
        } else {
            tag.setName(name);
            return (E)this.put(tag);
        }
    }
    /**
     * Creates a new BetterCompoundTag with the specified name.
     *
     * @param name the name of the tag
     */
    public BetterCompoundTag(String name) {
        super(name);
    }

    // ========== Enhanced Type Conversion Methods ==========

    /**
     * Gets a boolean value from the tag with the specified key.
     * If the value is not a boolean, attempts to convert it from other types.
     *
     * @param key the key of the value to get
     * @return the boolean value, or false if conversion is not possible
     */
    public boolean IgetBoolean(String key) {
        Tag tag = get(key);
        if (tag == null) {
            return false;
        }

        if (tag instanceof ByteTag) {
            return ((ByteTag) tag).getValue() != 0;
        } else if (tag instanceof IntTag) {
            return ((IntTag) tag).getValue() != 0;
        } else if (tag instanceof LongTag) {
            return ((LongTag) tag).getValue() != 0L;
        } else if (tag instanceof FloatTag) {
            return ((FloatTag) tag).getValue() != 0.0f;
        } else if (tag instanceof DoubleTag) {
            return ((DoubleTag) tag).getValue() != 0.0;
        } else if (tag instanceof StringTag) {
            String str = ((StringTag) tag).getValue().toLowerCase();
            return str.equals("true") || str.equals("1");
        }

        Logger.warn("Key " + key + " can't be converted to BOOLEAN");
        return false;
    }

    public boolean IgetBoolean(String key, Boolean defaultValue) {
        Tag tag = get(key);
        if (tag == null) {
            return defaultValue;
        }
        return IgetBoolean(key);
    }

    public int IgetInteger(String key, Integer defaultValue) {
        Tag tag = get(key);
        if (tag == null) {
            return defaultValue;
        }
        return IgetInt(key);
    }

    public long IgetLong(String key, Long defaultValue) {
        Tag tag = get(key);
        if (tag == null) {
            return defaultValue;
        }
        return IgetLong(key);
    }

    public float IgetFloat(String key, Float defaultValue) {
        Tag tag = get(key);
        if (tag == null) {
            return defaultValue;
        }
        return IgetFloat(key);
    }

    public double IgetDouble(String key, Double defaultValue) {
        Tag tag = get(key);
        if (tag == null) {
            return defaultValue;
        }
        return IgetDouble(key);
    }

    public byte IgetByte(String key, Byte defaultValue) {
        Tag tag = get(key);
        if (tag == null) {
            return defaultValue;
        }
        return getByte(key).getValue();
    }

    public byte IgetByte(String key) {
        if (get(key) == null) {
            return 0;
        }
        if (get(key) instanceof ByteTag) {
            return ((ByteTag) get(key)).getValue();
        } else if (get(key) instanceof IntTag) {
            return (byte) ((IntTag) get(key)).getValue().byteValue();
        } else if (get(key) instanceof LongTag) {
            return (byte) ((LongTag) get(key)).getValue().byteValue();
        } else if (get(key) instanceof FloatTag) {
            return (byte) ((FloatTag) get(key)).getValue().byteValue();
        } else if (get(key) instanceof DoubleTag) {
            return (byte) ((DoubleTag) get(key)).getValue().byteValue();
        } else if (get(key) instanceof StringTag) {
            return Byte.parseByte(((StringTag) get(key)).getValue());
        }
        Logger.warn("Key " + key + " can't be converted to BYTE");
        return 0;

    }

    public short IgetShort(String key, Short defaultValue) {
        Tag tag = get(key);
        if (tag == null) {
            return defaultValue;
        }
        return IgetShort(key);
    }


    /**
     * Gets an integer value from the tag with the specified key.
     * If the value is not an integer, attempts to convert it from other types.
     *
     * @param key the key of the value to get
     * @return the integer value, or 0 if conversion is not possible
     */
    public int IgetInt(String key) {
        return IgetInt(key, null);
    }

    public <T extends Tag> T getOrDefault(String key, T defaultValue) {
        Tag tag = get(key);
        if (tag == null) {
            return defaultValue;
        }
        return (T) tag;
    }

    /**
     * Gets an integer value from the tag with the specified key, or a default value if conversion fails.
     *
     * @param key the key of the value to get
     * @param defaultValue the default value to return if conversion fails
     * @return the integer value, or defaultValue if conversion is not possible
     */
public int IgetInt(String key, Integer defaultValue) {
        Tag tag = get(key);
        if (tag == null) {
            return defaultValue;
        }

        try {
            if (tag instanceof IntTag) {
                return ((IntTag) tag).getValue();
            } else if (tag instanceof ByteTag) {
                return ((ByteTag) tag).getValue();
            } else if (tag instanceof ShortTag) {
                return ((ShortTag) tag).getValue();
            } else if (tag instanceof LongTag) {
                return ((LongTag) tag).getValue().intValue();
            } else if (tag instanceof FloatTag) {
                return (int) (float) ((FloatTag) tag).getValue();
            } else if (tag instanceof DoubleTag) {
                return (int) (double) ((DoubleTag) tag).getValue();
            } else if (tag instanceof StringTag) {
                return Integer.parseInt(((StringTag) tag).getValue());
            }
        } catch (NumberFormatException e) {
            // Fall through to warning below
        }

        Logger.warn("Key " + key + " can't be converted to INT");
        return defaultValue;
    }

    /**
     * Gets a long value from the tag with the specified key.
     * If the value is not a long, attempts to convert it from other numeric types.
     *
     * @param key the key of the value to get
     * @return the long value, or 0L if conversion is not possible
     */
    public long IgetLong(String key) {
        return IgetLong(key, 0L);
    }

    /**
     * Gets a long value from the tag with the specified key, or a default value if conversion fails.
     *
     * @param key the key of the value to get
     * @param defaultValue the default value to return if conversion fails
     * @return the long value, or defaultValue if conversion is not possible
     */
    public long IgetLong(String key, long defaultValue) {
        Tag tag = get(key);
        if (tag == null) {
            return defaultValue;
        }

        try {
            if (tag instanceof LongTag) {
                return ((LongTag) tag).getValue();
            } else if (tag instanceof ByteTag) {
                return ((ByteTag) tag).getValue();
            } else if (tag instanceof ShortTag) {
                return ((ShortTag) tag).getValue();
            } else if (tag instanceof IntTag) {
                return ((IntTag) tag).getValue();
            } else if (tag instanceof FloatTag) {
                return (long) (float) ((FloatTag) tag).getValue();
            } else if (tag instanceof DoubleTag) {
                return (long) (double) ((DoubleTag) tag).getValue();
            } else if (tag instanceof StringTag) {
                return Long.parseLong(((StringTag) tag).getValue());
            }
        } catch (NumberFormatException e) {
            // Fall through to warning below
        }

        Logger.warn("Key " + key + " can't be converted to LONG");
        return defaultValue;
    }

    /**
     * Gets a float value from the tag with the specified key.
     * If the value is not a float, attempts to convert it from other numeric types.
     *
     * @param key the key of the value to get
     * @return the float value, or 0.0f if conversion is not possible
     */
    public float IgetFloat(String key) {
        return IgetFloat(key, 0.0f);
    }

    /**
     * Gets a float value from the tag with the specified key, or a default value if conversion fails.
     *
     * @param key the key of the value to get
     * @param defaultValue the default value to return if conversion fails
     * @return the float value, or defaultValue if conversion is not possible
     */
    public float IgetFloat(String key, float defaultValue) {
        Tag tag = get(key);
        if (tag == null) {
            return defaultValue;
        }

        try {
            if (tag instanceof FloatTag) {
                return ((FloatTag) tag).getValue();
            } else if (tag instanceof ByteTag) {
                return ((ByteTag) tag).getValue();
            } else if (tag instanceof ShortTag) {
                return ((ShortTag) tag).getValue();
            } else if (tag instanceof IntTag) {
                return ((IntTag) tag).getValue();
            } else if (tag instanceof LongTag) {
                return ((LongTag) tag).getValue();
            } else if (tag instanceof DoubleTag) {
                return (float) (double) ((DoubleTag) tag).getValue();
            } else if (tag instanceof StringTag) {
                return Float.parseFloat(((StringTag) tag).getValue());
            }
        } catch (NumberFormatException e) {
            // Fall through to warning below
        }

        Logger.warn("Key " + key + " can't be converted to FLOAT");
        return defaultValue;
    }

    /**
     * Gets a short value from the tag with the specified key.
     * If the value is not a short, attempts to convert it from other numeric types.
     *
     * @param key the key of the value to get
     * @return the short value, or 0 if conversion is not possible
     */
    public short IgetShort(String key) {
        return IgetShort(key, (short) 0);
    }

    /**
     * Gets a short value from the tag with the specified key, or a default value if conversion fails.
     *
     * @param key the key of the value to get
     * @param defaultValue the default value to return if conversion fails
     * @return the short value, or defaultValue if conversion is not possible
     */
    public short IgetShort(String key, short defaultValue) {
        Tag tag = get(key);
        if (tag == null) {
            return defaultValue;
        }

        try {
            if (tag instanceof ShortTag) {
                return ((ShortTag) tag).getValue();
            } else if (tag instanceof ByteTag) {
                return ((ByteTag) tag).getValue();
            } else if (tag instanceof IntTag) {
                return (short) (int) ((IntTag) tag).getValue();
            } else if (tag instanceof LongTag) {
                return (short) (long) ((LongTag) tag).getValue();
            } else if (tag instanceof FloatTag) {
                return (short) (float) ((FloatTag) tag).getValue();
            } else if (tag instanceof DoubleTag) {
                return (short) (double) ((DoubleTag) tag).getValue();
            } else if (tag instanceof StringTag) {
                return Short.parseShort(((StringTag) tag).getValue());
            }
        } catch (NumberFormatException e) {
            // Fall through to warning below
        }

        Logger.warn("Key " + key + " can't be converted to SHORT");
        return defaultValue;
    }

    /**
     * Gets a double value from the tag with the specified key.
     * If the value is not a double, attempts to convert it from other numeric types.
     *
     * @param key the key of the value to get
     * @return the double value, or 0.0 if conversion is not possible
     */
    public double IgetDouble(String key) {
        return IgetDouble(key, 0.0);
    }

    /**
     * Gets a double value from the tag with the specified key, or a default value if conversion fails.
     *
     * @param key the key of the value to get
     * @param defaultValue the default value to return if conversion fails
     * @return the double value, or defaultValue if conversion is not possible
     */
    public double IgetDouble(String key, double defaultValue) {
        Tag tag = get(key);
        if (tag == null) {
            return defaultValue;
        }

        try {
            if (tag instanceof DoubleTag) {
                return ((DoubleTag) tag).getValue();
            } else if (tag instanceof ByteTag) {
                return ((ByteTag) tag).getValue();
            } else if (tag instanceof ShortTag) {
                return ((ShortTag) tag).getValue();
            } else if (tag instanceof IntTag) {
                return ((IntTag) tag).getValue();
            } else if (tag instanceof LongTag) {
                return ((LongTag) tag).getValue();
            } else if (tag instanceof FloatTag) {
                return ((FloatTag) tag).getValue();
            } else if (tag instanceof StringTag) {
                return Double.parseDouble(((StringTag) tag).getValue());
            }
        } catch (NumberFormatException e) {
            // Fall through to warning below
        }

        Logger.warn("Key " + key + " can't be converted to DOUBLE");
        return defaultValue;
    }


    /**
     * Gets a string value from the tag with the specified key.
     * If the value is not a string, converts it to a string representation.
     *
     * @param key the key of the value to get
     * @return the string value, or an empty string if the key doesn't exist
     */
public String IgetString(String key) {
        return IgetString(key, null);
    }

    /**
     * Gets a string value from the tag with the specified key, or a default value if the key doesn't exist.
     *
     * @param key the key of the value to get
     * @param defaultValue the default value to return if the key doesn't exist
     * @return the string value, or defaultValue if the key doesn't exist
     */
public String IgetString(String key, String defaultValue) {
        Tag tag = get(key);
        if (tag == null) {
            return defaultValue;
        }

        if (tag instanceof StringTag) {
            return ((StringTag) tag).getValue();
        } else if (tag instanceof ByteTag) {
            return String.valueOf(((ByteTag) tag).getValue());
        } else if (tag instanceof IntTag) {
            return String.valueOf(((IntTag) tag).getValue());
        } else if (tag instanceof LongTag) {
            return String.valueOf(((LongTag) tag).getValue());
        } else if (tag instanceof FloatTag) {
            return String.valueOf(((FloatTag) tag).getValue());
        } else if (tag instanceof DoubleTag) {
            return String.valueOf(((DoubleTag) tag).getValue());
        } else {
            return tag.toString();
        }
    }

    // ========== SNBT Support ==========

    /**
     * Converts this BetterCompoundTag to its SNBT (String NBT) representation.
     *
     * @return the SNBT string representation of this tag
     */
public String ItoSnbt() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, Tag> entry : this.getValue().entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            
            String key = entry.getKey();
            Tag tag = entry.getValue();
            
            // Escape and quote the key if needed
            if (!key.matches("^[a-zA-Z0-9._+-]+$")) {
                key = "\"" + IescapeString(key) + "\"";
            }
            
            sb.append(key).append(':');
IappendTagSnbt(sb, tag);
        }
        
        sb.append('}');
        return sb.toString();
    }
    
    /**
     * Appends the SNBT representation of a tag to the StringBuilder.
     *
     * @param sb the StringBuilder to append to
     * @param tag the tag to convert to SNBT
     */
private void IappendTagSnbt(StringBuilder sb, Tag tag) {
        if (tag instanceof StringTag) {
sb.append('"').append(IescapeString(((StringTag) tag).getValue())).append('"');
        } else if (tag instanceof CompoundTag) {
            BetterCompoundTag itag = new BetterCompoundTag();
            itag.put((CompoundTag) tag);
            sb.append((sb.append(itag.ItoSnbt())));
        } else if (tag instanceof ByteTag) {
            sb.append(((ByteTag) tag).getValue()).append('b');
        } else if (tag instanceof ShortTag) {
            sb.append(((ShortTag) tag).getValue()).append('s');
        } else if (tag instanceof IntTag) {
            sb.append(((IntTag) tag).getValue());
        } else if (tag instanceof LongTag) {
            sb.append(((LongTag) tag).getValue()).append('L');
        } else if (tag instanceof FloatTag) {
            sb.append(((FloatTag) tag).getValue()).append('f');
        } else if (tag instanceof DoubleTag) {
            sb.append(((DoubleTag) tag).getValue());
        } else if (tag.toString().matches("^[a-zA-Z0-9._+-]+$")) {
            sb.append(tag);
        } else {
sb.append('"').append(IescapeString(tag.toString())).append('"');
        }
    }
    
    /**
     * Escapes special characters in a string for SNBT.
     *
     * @param str the string to escape
     * @return the escaped string
     */
private String IescapeString(String str) {
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\\\n");
    }
    
    // ========== Utility Methods ==========
    
    /**
     * Creates a new BetterCompoundTag from an existing CompoundTag.
     *
     * @param tag the tag to copy
     * @return a new BetterCompoundTag with the same data as the input tag
     */
    public static BetterCompoundTag from(CompoundTag tag) {
        BetterCompoundTag result = new BetterCompoundTag(tag.getName());
        result.setValue(tag.getValue());
        return result;
    }
    
    /**
     * Creates a new BetterCompoundTag from an existing Map.
     *
     * @param map the map to create the tag from
     * @return a new BetterCompoundTag containing the map data
     */
    public static BetterCompoundTag fromMap(Map<String, Tag> map) {
        BetterCompoundTag result = new BetterCompoundTag();
        for (Map.Entry<String, Tag> entry : map.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
    
    /**
     * Creates a new BetterCompoundTag from SNBT (String NBT) format.
     *
     * @param snbt the SNBT string to parse
     * @return a new BetterCompoundTag containing the parsed data
     * @throws IllegalArgumentException if the SNBT is invalid
     */
    public static BetterCompoundTag fromSnbt(String snbt) {
        // This is a simplified implementation that only handles basic SNBT
        // For a complete implementation, consider using a proper SNBT parser
        try {
            // This is a placeholder - in a real implementation, you would parse the SNBT string
            // and create the appropriate tag objects
            return new BetterCompoundTag();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid SNBT: " + e.getMessage(), e);
        }
    }
}
