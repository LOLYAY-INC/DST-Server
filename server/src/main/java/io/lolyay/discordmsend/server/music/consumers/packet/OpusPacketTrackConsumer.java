package io.lolyay.discordmsend.server.music.consumers.packet;

import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.AudioS2CPacket;
import io.lolyay.discordmsend.network.types.ClientFeatures;
import io.lolyay.discordmsend.obj.AudioCodec;
import io.lolyay.discordmsend.server.music.consumers.AbstractTrackConsumer;
import io.lolyay.discordmsend.server.music.players.GuildPlayerInstance;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;


@Slf4j
public class OpusPacketTrackConsumer extends AbstractTrackConsumer {
    //TODO: 20ms is not always good, ideally should be configurable by the client, and adding a burst of frames at the start to always keep a client sided jitter buffer configurable by the client would be great
    private static final long FRAME_NS = 20_000_000L; // 20ms in nanoseconds
    private static final byte[] OPUS_SILENCE = {(byte) 0xF8, (byte) 0xFF, (byte) 0xFE};

    private final boolean udpMode;
    private final AtomicLong sequence = new AtomicLong(0);

    private volatile boolean running = false;

    private long nextSendNs = 0;

    public OpusPacketTrackConsumer(GuildPlayerInstance parent) {
        super(parent, parent.getGuildId());
        this.udpMode = parent.getParent().getOwner().getUserData() != null &&
                parent.getParent().getOwner().getUserData().features().contains(
                        ClientFeatures.Feature.UDP_ME_PLZ);
        log.info("Created OpusPacketTrackConsumer for guild {} (UDP mode: {})", getGuildId(), udpMode);
    }

    @Override
    protected void init() {
        sequence.set(0);
    }

    @Override
    protected void start() {
        running = true;
        nextSendNs = System.nanoTime();
        log.info("OpusPacketTrackConsumer started for guild {}", getGuildId());
    }

    @Override
    protected void cleanUp() {
        running = false;
        getOpusQueue().clear();
    }

    @Override
    protected void end() {
        running = false;
    }

    @Override
    public void tick() {
        if (!running || isPaused()) return;

        long now = System.nanoTime();
        if (now < nextSendNs) return;

        byte[] frame = getOpusQueue().poll();
        if (frame == null) {
            if (udpMode) {
                nextSendNs += FRAME_NS;
                return;
            }
            frame = OPUS_SILENCE;
        }
        try {
            AudioS2CPacket packet = new AudioS2CPacket(getGuildId(), AudioCodec.OPUS_MAX, frame, sequence.getAndIncrement());
            getPlayerInstance().getParent().getOwner().sendPacket(packet);
        } catch (Exception e) {
            log.error("Error sending opus packet for guild {}: {}", getGuildId(), e.getMessage());
        }

        nextSendNs += FRAME_NS;
    }
}
