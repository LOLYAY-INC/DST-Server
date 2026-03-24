package io.lolyay.discordmsend.server.music.pools.player;

import io.lolyay.discordmsend.server.music.players.GuildPlayerInstance;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class GuildPlayerThread extends Thread {

    private final int threadIndex;
    private final int numThreads;
    private final List<GuildPlayerInstance> players;
    private volatile boolean running = true;

    public GuildPlayerThread(int threadIndex, int numThreads, List<GuildPlayerInstance> players) {
        this.threadIndex = threadIndex;
        this.numThreads = numThreads;
        this.players = players;
        setName("GuildPlayerThread-" + threadIndex);
        setDaemon(true);
    }

    public void shutdown() {
        running = false;
        interrupt();
    }

    @Override
    public void run() {
        log.debug("{} started", getName());
        while (running) {
            List<GuildPlayerInstance> snapshot = players; // CopyOnWriteArrayList read is cheap
            for (int i = threadIndex; i < snapshot.size(); i += numThreads) {
                GuildPlayerInstance player = snapshot.get(i);
                try {
                    player.tick();
                } catch (Exception e) {
                    log.error("Error ticking GuildPlayerInstance for guild {}: {}",
                            player.getGuildId(), e.getMessage(), e);
                }
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.debug("{} exited cleanly", getName());
    }
}
