package io.lolyay.discordmsend.server.music.pools.opus;

import io.github.jaredmdobson.concentus.OpusException;
import io.lolyay.discordmsend.network.types.TrackMetadata;
import io.lolyay.discordmsend.obj.EndReason;
import io.lolyay.discordmsend.server.music.players.GuildPlayerInstance;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;

@Slf4j
@Getter
public class OpusEncodingTask {

    private static final int FRAME_SIZE = 960;
    private static final int MAX_OPUS_BYTES = 4000;

    private final GuildPlayerInstance player;
    private final io.github.jaredmdobson.concentus.OpusEncoder encoder;
    private final int framesPerTick;
    private final OpusEncoderPool pool;

    @Setter
    private volatile boolean running = false;

    private int idleTicks = 0;
    private static final int IDLE_TICK_LIMIT = 500;

    @SneakyThrows
    public OpusEncodingTask(GuildPlayerInstance player, int framesPerTick, OpusEncoderPool pool) {
        this.player = player;
        this.encoder = OpusEncoderPool.createEncoder();
        this.framesPerTick = framesPerTick;
        this.pool = pool;
    }

    public void process() {
        if (!running) return;

        ArrayList<short[]> pcmFrames = player.getPcmFrames();
        boolean streamEnded = player.getConsumer().getAudioProvider().isStreamEnded();

        for (int i = 0; i < framesPerTick; i++) {
            if (player.getOpusQueue().remainingCapacity() == 0) return;

            int pos = player.getAndIncrementPosition();
            short[] frame;

            synchronized (pcmFrames) {
                if (pos >= pcmFrames.size()) {
                    player.getEncodePosition().compareAndSet(pos + 1, pos);

                    if (streamEnded) {
                        log.debug("Encoding complete for guild {} ({} frames)", player.getGuildId(), pos);
                        TrackMetadata track =
                                player.getConsumer().getAudioProvider().getPlayingTrack();
                        if (track != null) {
                            log.info("Track '{}' finished naturally for guild {}", track.trackName(), player.getGuildId());
                            player.onTrackEnd(track, EndReason.FINISHED);
                        }
                        stop();
                    } else {
                        idleTicks++;
                        if (idleTicks >= IDLE_TICK_LIMIT) {
                            log.warn("Encoder idle for {}ms for guild {} — stopping stale task",
                                    IDLE_TICK_LIMIT, player.getGuildId());
                            stop();
                        }
                    }
                    return;
                }
                frame = pcmFrames.get(pos);
            }

            idleTicks = 0;

            float vol = player.getConsumer().getVolume();
            if (vol != 1F) {
                short[] scaled = Arrays.copyOf(frame, frame.length);
                for (int j = 0; j < scaled.length; j++) {
                    scaled[j] = (short) Math.max(Short.MIN_VALUE,
                            Math.min(Short.MAX_VALUE, (int) (scaled[j] * vol)));
                }
                frame = scaled;
            }

            try {
                byte[] buf = new byte[MAX_OPUS_BYTES];
                int encoded = encoder.encode(frame, 0, FRAME_SIZE, buf, 0, buf.length);
                byte[] packet = Arrays.copyOf(buf, encoded);
                player.getOpusQueue().offer(packet);
            } catch (OpusException e) {
                log.warn("Opus encode error for guild {} — dropping frame: {}", player.getGuildId(), e.getMessage());
            }
        }
    }

    public void stop() {
        if (!running) return;
        running = false;
        player.getOpusQueue().clear();
        pool.removeTask(this);
        log.debug("OpusEncodingTask stopped for guild {}", player.getGuildId());
    }
}