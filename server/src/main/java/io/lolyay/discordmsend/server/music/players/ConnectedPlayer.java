package io.lolyay.discordmsend.server.music.players;

import io.lolyay.discordmsend.network.types.ClientFeatures;
import io.lolyay.discordmsend.server.network.ConnectedClient;
import io.lolyay.discordmsend.server.DstServer;


import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import moe.kyokobot.koe.Koe;
import moe.kyokobot.koe.KoeClient;

@Slf4j
@Getter
@Setter
public class ConnectedPlayer {
    private final String userId;
    private final KoeClient koe;
    private final Long2ObjectOpenHashMap<GuildPlayerInstance> players = new Long2ObjectOpenHashMap<>();
    private final DstServer dstServer;
    private final ConnectedClient owner;
    private float defaultVolume = 0.1f;

    public ConnectedPlayer(String userId, Koe koe, DstServer dstServer, ConnectedClient owner) {
        this.userId = userId;
        this.koe = koe.newClient(Long.parseLong(userId));
        this.dstServer = dstServer;
        this.owner = owner;
    }

    public GuildPlayerInstance getOrCreatePlayer(long guildId) {
        return players.computeIfAbsent(guildId, (id) -> {
            if (owner.getUserData() == null) {
                log.warn("Attempted to create player for guild " + guildId + " before UserData is set!");
                throw new RuntimeException("Client not identified yet (UserData is null). Please send EncHello first.");
            }

            boolean isDiscordBot = owner.getUserData().features().contains(
                    ClientFeatures.Feature.IS_DISCORD_BOT);

            GuildPlayerInstance player;
            if (!isDiscordBot) {
                log.info("Creating SpecialThreadPlayer for guild ID " + guildId + " (non-Discord bot, direct packets)");
                player = new GuildPlayerInstance(guildId, this);
            } else {
                log.debug("Creating KOE-based SenderPlayer for guild ID " + guildId + " (Discord bot)");
                player = new GuildPlayerInstance(guildId, this, koe.createConnection(guildId));
            }

            dstServer.getGuildPlayerPool().register(player);
            return player;
        });
    }

    public void destroy() {
        for (GuildPlayerInstance player : players.values()) {
            dstServer.getGuildPlayerPool().unregister(player);
        }
        players.clear();
        koe.close();
    }

}
