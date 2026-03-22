package io.lolyay.discordmsend.server.addon;

import io.lolyay.discordmsend.server.TriPredicate;
import io.lolyay.discordmsend.server.cache.AudioCacheManager;
import io.lolyay.discordmsend.server.music.players.GuildPlayerInstance;
import io.lolyay.discordmsend.server.music.providers.IProvider;

import java.util.function.BiFunction;

public interface ProviderContainer {
    BiFunction<GuildPlayerInstance, AudioCacheManager, ? extends IProvider> providerFactory();
    TriPredicate<GuildPlayerInstance, AudioCacheManager, String> playablePredicate();
}
