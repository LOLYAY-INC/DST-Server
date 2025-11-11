package io.lolyay.discordmsend.server.music.players;


import io.lolyay.discordmsend.network.types.ClientFeatures;
import io.lolyay.discordmsend.server.network.ConnectedClient;
import io.lolyay.discordmsend.server.Server;
import io.lolyay.discordmsend.util.logging.Logger;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import moe.kyokobot.koe.Koe;
import moe.kyokobot.koe.KoeClient;

public class TrackPlayerClient {
    private final String userId;
    private final KoeClient koe;
    private final Long2ObjectOpenHashMap<TrackPlayerInstance> players = new Long2ObjectOpenHashMap<>();
    private final Server server;
    private final ConnectedClient owner;
    private float defaultVolume = 0.1f;

    public Long2ObjectOpenHashMap<TrackPlayerInstance> getPlayers() {
        return players;
    }

    public TrackPlayerClient(String userId, Koe koe, Server server, ConnectedClient owner) {
        this.userId = userId;
        this.koe = koe.newClient(Long.parseLong(userId));
        this.server = server;
        this.owner = owner;
    }

    public ConnectedClient getOwner() {
        return owner;
    }

    public Server getServer() {
        return server;
    }

    public TrackPlayerInstance getOrCreatePlayer(long guildId){
        return players.computeIfAbsent(guildId,(id) -> {
            // Check if client is a Discord bot
            boolean isDiscordBot = owner.getUserData() != null && 
                                  owner.getUserData().features().contains(
                                      ClientFeatures.Feature.IS_DISCORD_BOT);
            
            // If NOT a Discord bot, use SpecialThreadPlayer (direct audio packets)
            if (!isDiscordBot) {
                Logger.info("Creating SpecialThreadPlayer for guild ID " + guildId + " (non-Discord bot, direct packets)");
                return new TrackPlayerInstance(guildId, this);
            } else {
                // Discord bot: use normal KOE-based player
                Logger.debug("Creating KOE-based SenderPlayer for guild ID " + guildId + " (Discord bot)");
                return new TrackPlayerInstance(guildId, this, koe.createConnection(guildId));
            }
        });
    }

    public void destroy(){
        koe.close();
    }
    public String getUserId() {
        return userId;
    }

    public void setDefaultVolume(float defaultVolume) {
        this.defaultVolume = defaultVolume;
    }

    public float getDefaultVolume() {
        return defaultVolume;
    }
}
