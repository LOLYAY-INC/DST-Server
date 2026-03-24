package io.lolyay.discordmsend.server.music.consumers.packet;

import io.lolyay.discordmsend.network.protocol.codec.AudioConverter;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.AudioS2CPacket;
import io.lolyay.discordmsend.network.types.ClientFeatures;
import io.lolyay.discordmsend.obj.AudioCodec;
import io.lolyay.discordmsend.server.music.consumers.AbstractTrackConsumer;
import io.lolyay.discordmsend.server.music.players.GuildPlayerInstance;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;


@Slf4j
public class PcmPacketTrackConsumer extends AbstractTrackConsumer {

    //TODO: same as OpusPacketTrackConsumer
    private static final long FRAME_NS = 20_000_000L;

    private final boolean udpMode;
    private final AtomicLong sequence = new AtomicLong(0);

    private volatile boolean running = false;
    private long nextSendNs = 0;

    public PcmPacketTrackConsumer(GuildPlayerInstance parent) {
        super(parent, parent.getGuildId());
        this.udpMode = parent.getParent().getOwner().getUserData() != null &&
                parent.getParent().getOwner().getUserData().features().contains(
                        ClientFeatures.Feature.UDP_ME_PLZ);
        log.info("Created PcmPacketTrackConsumer for guild {} (UDP mode: {})", getGuildId(), udpMode);
    }

    @Override
    protected boolean shouldEncodeToOpus() {
        return false;
    }

    @Override
    protected void init() {
        sequence.set(0);
    }

    @Override
    protected void start() {
        running = true;
        nextSendNs = System.nanoTime();
        log.info("PcmPacketTrackConsumer started for guild {}", getGuildId());
    }

    @Override
    protected void cleanUp() {
        running = false;
    }

    @Override
    protected void end() {
        running = false;
    }

    @Override
    public void tick() {
        if (!running || isPaused()) return;

        ArrayList<short[]> pcmFrames = getPlayerInstance().getPcmFrames();

        if (udpMode) {
            int pos = getPlayerInstance().getAndIncrementPosition();
            short[] frame;
            synchronized (pcmFrames) {
                if (pos >= pcmFrames.size()) {
                    getPlayerInstance().getEncodePosition().compareAndSet(pos + 1, pos);
                    return;
                }
                frame = pcmFrames.get(pos);
            }
            sendPcmFrame(frame);
            return;
        }

        long now = System.nanoTime();
        if (now < nextSendNs) return;

        int pos = getPlayerInstance().getAndIncrementPosition();
        short[] frame;
        synchronized (pcmFrames) {
            if (pos >= pcmFrames.size()) {
                getPlayerInstance().getEncodePosition().compareAndSet(pos + 1, pos);
                nextSendNs += FRAME_NS;
                return;
            }
            frame = pcmFrames.get(pos);
        }

        sendPcmFrame(frame);
        nextSendNs += FRAME_NS;
    }

    private void sendPcmFrame(short[] samples) {
        float vol = getVolume();
        if (vol != 1F) {
            short[] scaled = new short[samples.length];
            for (int i = 0; i < samples.length; i++) {
                scaled[i] = (short) Math.max(Short.MIN_VALUE,
                        Math.min(Short.MAX_VALUE, (int) (samples[i] * vol)));
            }
            samples = scaled;
        }

        byte[] buf = new byte[samples.length * 2];
        AudioConverter.convertToByteArray(samples, buf);
        AudioS2CPacket packet = new AudioS2CPacket(getGuildId(), AudioCodec.PCM_MAX, buf, sequence.getAndIncrement());
        getPlayerInstance().getParent().getOwner().sendPacket(packet);
    }
}