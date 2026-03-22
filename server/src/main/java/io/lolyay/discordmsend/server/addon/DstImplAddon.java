package io.lolyay.discordmsend.server.addon;

import io.lolyay.discordmsend.server.DstServer;
import io.lolyay.discordmsend.server.music.providers.IProvider;
import io.lolyay.discordmsend.server.music.providers.ISearcher;
import io.lolyay.discordmsend.server.music.providers.ProviderPool;

import java.util.List;

public abstract class DstImplAddon {
    public abstract void onEnable(DstServer dstServer);
    public abstract void onDisable(DstServer dstServer);
    public abstract String getId();
    public abstract String getVersion();

    public List<ProviderContainer> getProviders() { return List.of(); }
    public List<ISearcher> getSearchers() { return List.of(); }

}
