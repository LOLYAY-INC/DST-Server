package io.lolyay.discordmsend.server.music.pools.player;

import io.lolyay.discordmsend.server.music.players.GuildPlayerInstance;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class GuildPlayerPool {

    private final GuildPlayerThread[] threads;
    private final CopyOnWriteArrayList<GuildPlayerInstance> players = new CopyOnWriteArrayList<>();

    public GuildPlayerPool(int numThreads) {
        this.threads = new GuildPlayerThread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new GuildPlayerThread(i, numThreads, players);
            threads[i].start();
        }
        log.info("GuildPlayerPool started with {} threads", numThreads);
    }

    public void register(GuildPlayerInstance player) {
        players.add(player);
        log.debug("Registered GuildPlayerInstance for guild {} (total: {})", player.getGuildId(), players.size());
    }
    public void unregister(GuildPlayerInstance player) {
        players.remove(player);
        log.debug("Unregistered GuildPlayerInstance for guild {} (remaining: {})", player.getGuildId(), players.size());
    }

    public void shutdownAll() {
        for (GuildPlayerThread thread : threads) {
            thread.shutdown();
        }
        players.clear();
        log.info("GuildPlayerPool shut down");
    }
}
