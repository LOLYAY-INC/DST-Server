package io.lolyay.discordmsend.server.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class HighResolutionTimer {

    private static volatile Thread pinThread;

    private HighResolutionTimer() {}

    public static synchronized void enable() {
        if (pinThread != null) return;
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) return;

        Thread t = new Thread(() -> {
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }, "HighResTimerPin");
        t.setDaemon(true);
        t.start();
        pinThread = t;
    }
}
