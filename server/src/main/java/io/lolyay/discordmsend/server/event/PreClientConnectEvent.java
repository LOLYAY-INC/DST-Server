package io.lolyay.discordmsend.server.event;

import dev.dewy.nbt.tags.collection.CompoundTag;
import io.lolyay.discordmsend.network.types.ModdedInfo;
import io.lolyay.discordmsend.server.DstServer;
import io.lolyay.discordmsend.server.addon.DstImplAddon;
import io.lolyay.discordmsend.server.network.ConnectedClient;
import lombok.Getter;

@Getter
public class PreClientConnectEvent extends ClientEvent {
    private final ModdedInfo moddedInfo;

    private String cR;
    public PreClientConnectEvent(DstServer server, ConnectedClient client, ModdedInfo info) {
        super(server, client);
        this.moddedInfo = info;
    }

    public void addInfo(DstImplAddon addon, CompoundTag tag) {
        moddedInfo.getModInfo().put(addon.getId(), tag);
    }

    public void setCancelled(boolean cancel, String reason) {
        super.setCancelled(cancel);
        this.cR = reason;
    }

    @Override
    public void setCancelled(boolean cancel) {
        super.setCancelled(cancel);
        this.cR = "Disconnected";
    }
}
