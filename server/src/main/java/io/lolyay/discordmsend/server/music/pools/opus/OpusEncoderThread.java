package io.lolyay.discordmsend.server.music.pools.opus;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class OpusEncoderThread extends Thread {

    private final CopyOnWriteArrayList<OpusEncodingTask> tasks = new CopyOnWriteArrayList<>();
    private volatile boolean running = true;

    public OpusEncoderThread() {
        this.setName("OpusEncoderThread-" + getId());
        this.setDaemon(true);
    }

    public void addTask(OpusEncodingTask task) {
        tasks.add(task);
    }

    public boolean removeTask(OpusEncodingTask task) {
        return tasks.remove(task);
    }

    public int getTaskCount() {
        return tasks.size();
    }

    public void shutdown() {
        running = false;
        this.interrupt();
    }

    @Override
    public void run() {
        while (running) {
            for (OpusEncodingTask task : tasks) {
                if (task.isRunning()) {
                    task.process();
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