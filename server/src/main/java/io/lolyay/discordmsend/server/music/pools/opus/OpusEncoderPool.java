package io.lolyay.discordmsend.server.music.pools.opus;

import io.github.jaredmdobson.concentus.*;
import io.lolyay.discordmsend.server.music.players.GuildPlayerInstance;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class OpusEncoderPool {

    private final int maxThreads;
    private final int framesPerTick;
    private final List<OpusEncoderThread> threads = new ArrayList<>();
    private final Object lock = new Object();

    public OpusEncoderPool(int maxThreads, int framesPerTick) {
        this.maxThreads = maxThreads;
        this.framesPerTick = framesPerTick;
    }

    public OpusEncodingTask registerTask(GuildPlayerInstance player) {
        OpusEncodingTask task = new OpusEncodingTask(player, framesPerTick, this);

        synchronized (lock) {
            if (threads.size() < maxThreads) {
                OpusEncoderThread thread = new OpusEncoderThread();
                thread.addTask(task);
                thread.start();
                threads.add(thread);
                log.debug("Spawned new OpusEncoderThread (total: {})", threads.size());
            } else {
                OpusEncoderThread least = threads.stream()
                        .min(Comparator.comparingInt(OpusEncoderThread::getTaskCount))
                        .orElseThrow();
                least.addTask(task);
                log.debug("Assigned task to existing thread (load: {})", least.getTaskCount());
            }
        }

        task.setRunning(true);
        return task;
    }

    void removeTask(OpusEncodingTask task) {
        synchronized (lock) {
            threads.removeIf(thread -> {
                if (thread.removeTask(task)) {
                    if (thread.getTaskCount() == 0) {
                        thread.shutdown();
                        log.debug("Shut down idle OpusEncoderThread (remaining: {})", threads.size() - 1);
                        return true;
                    }
                }
                return false;
            });
        }
    }


    public void shutdownAll() {
        synchronized (lock) {
            threads.forEach(OpusEncoderThread::shutdown);
            threads.clear();
        }
    }


    public static OpusEncoder createEncoder() throws OpusException {
        OpusEncoder encoder = new OpusEncoder(48000, 2, OpusApplication.OPUS_APPLICATION_AUDIO);
        encoder.setBitrate(OpusConstants.OPUS_BITRATE_MAX);
        encoder.setSignalType(OpusSignal.OPUS_SIGNAL_MUSIC);
        encoder.setBandwidth(OpusBandwidth.OPUS_BANDWIDTH_FULLBAND);
        encoder.setApplication(OpusApplication.OPUS_APPLICATION_AUDIO);
        encoder.setMaxBandwidth(OpusBandwidth.OPUS_BANDWIDTH_FULLBAND);
        encoder.setUseVBR(true);
        encoder.setUseConstrainedVBR(false);
        encoder.setUseDTX(false);
        encoder.setPacketLossPercent(5);
        encoder.setLSBDepth(24);
        encoder.setPredictionDisabled(false);
        encoder.setComplexity(10);
        return encoder;
    }
}