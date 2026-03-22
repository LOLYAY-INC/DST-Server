package io.lolyay.discordmsend.network.types;

import dev.dewy.nbt.tags.collection.CompoundTag;
import io.lolyay.discordmsend.network.protocol.codec.PacketByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import lombok.Getter;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
public class ModdedInfo {
    private boolean isModded = false;
    private int numOfMods = 0;
    private final Object2ObjectArrayMap<String, CompoundTag> modInfo = new Object2ObjectArrayMap<>();

    public ModdedInfo(PacketByteBuf buf) {
        this.isModded = buf.readBoolean();
        this.numOfMods = buf.readVarInt();
        buf.readArray(ib -> Map.entry(buf.readString(), buf.readAnyNbt()), numOfMods)
                .forEach(entry -> modInfo.put(entry.getKey(), (CompoundTag) entry.getValue()));
    }


    public ModdedInfo(Map<String ,String> modIdVersion){
        this.isModded = false;
        this.numOfMods = modIdVersion.size();
        modIdVersion
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .forEach(entry -> {
                    this.isModded = true;
                    CompoundTag tag = new CompoundTag();
                    tag.putString("version", entry.getValue());
                    modInfo.put(entry.getKey(), tag);
                });
    }

    public CompoundTag getModInfo(String modId) {
        return modInfo.get(modId);
    }

    public void write(PacketByteBuf buf) {
        buf.writeBoolean(isModded);
        buf.writeVarInt(numOfMods);
        for(Map.Entry<String, CompoundTag> entry : modInfo.entrySet()) {
            buf.writeString(entry.getKey());
            buf.writeAnyNbt(entry.getValue());
        }
    }

    public boolean isModPresent(String mod){
        return modInfo.containsKey(mod);
    }

    public Collection<String > getModNames(){
        return modInfo.keySet();
    }

    public String getModVersion(String modId) {
        if(!isModPresent(modId)) return null;
        return modInfo.get(modId).getString("version").getValue();
    }

    public String toString(){
        return "ModdedInfo{" + "isModded=" + isModded + ", numOfMods=" + numOfMods + '}';
    }
}
