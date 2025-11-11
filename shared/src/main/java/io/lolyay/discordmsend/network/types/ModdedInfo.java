package io.lolyay.discordmsend.network.types;

import dev.dewy.nbt.tags.collection.CompoundTag;
import io.lolyay.discordmsend.network.protocol.codec.PacketByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;

import java.util.Collection;
import java.util.Map;

public class ModdedInfo {
    private boolean isModded = false;
    private int numOfMods = 0;
    private Object2ObjectArrayMap<String, CompoundTag> modInfo = new Object2ObjectArrayMap<>();

    public ModdedInfo(PacketByteBuf buf) {
        this.isModded = buf.readBoolean();
        this.numOfMods = buf.readVarInt();
        buf.readArray(ib -> Map.entry(buf.readString(), buf.readAnyNbt()), numOfMods)
                .forEach(entry -> modInfo.put(entry.getKey(), (CompoundTag) entry.getValue()));
    }

    public ModdedInfo(Map<String ,CompoundTag> modInfo){
        this.isModded = true;
        this.numOfMods = modInfo.size();
        this.modInfo.putAll(modInfo);
    }

    public ModdedInfo(){
        this.isModded = false;
        this.numOfMods = 0;
    }

    public boolean isModded() {
        return isModded;
    }

    public int getNumOfMods() {
        return numOfMods;
    }

    public Object2ObjectArrayMap<String, CompoundTag> getModInfo() {
        return modInfo;
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

    public String toString(){
        return "ModdedInfo{" + "isModded=" + isModded + ", numOfMods=" + numOfMods + '}';
    }
}
