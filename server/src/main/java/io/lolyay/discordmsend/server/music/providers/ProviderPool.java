package io.lolyay.discordmsend.server.music.providers;

import io.lolyay.discordmsend.network.types.TrackMetadata;
import io.lolyay.discordmsend.server.TriPredicate;
import io.lolyay.discordmsend.server.cache.AudioCacheManager;
import io.lolyay.discordmsend.server.music.players.GuildPlayerInstance;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProviderPool {
    @Getter
    private static final ProviderPool instance = new ProviderPool();

    private final Map<BiFunction<GuildPlayerInstance, AudioCacheManager, ? extends IProvider>, TriPredicate<GuildPlayerInstance, AudioCacheManager, String>> providers = new Object2ObjectOpenHashMap<>();
    private final List<ISearcher> searchers = new ObjectArrayList<>();

    public void register(BiFunction<GuildPlayerInstance, AudioCacheManager, ? extends IProvider> provider, TriPredicate<GuildPlayerInstance, AudioCacheManager, String> playablePredicate) {
        providers.put(provider, playablePredicate);
    }

    public void register(ISearcher searcher) {
        searchers.add(searcher);
    }

    public void unregister(BiFunction<GuildPlayerInstance, AudioCacheManager, ? extends IProvider> provider) {
        providers.remove(provider);
    }

    public IProvider createProvider(GuildPlayerInstance playerInstance, AudioCacheManager cacheManager, String query) {
        for (Map.Entry<BiFunction<GuildPlayerInstance, AudioCacheManager, ? extends IProvider>, TriPredicate<GuildPlayerInstance, AudioCacheManager, String>> provider : providers.entrySet()) {
            if (provider.getValue().test(playerInstance, cacheManager, query)) {
                return provider.getKey().apply(playerInstance, cacheManager);
            }
        }
        return null;
    }

    public List<TrackMetadata> search(String query, int limit) {
        List<TrackMetadata> results = new ObjectArrayList<>();
        for (ISearcher searcher : searchers) {
            if(searcher.canSearch(query)) {
                results.addAll(searcher.search(query, limit));
                return results;
            }
        }
        return results;
    }
}
