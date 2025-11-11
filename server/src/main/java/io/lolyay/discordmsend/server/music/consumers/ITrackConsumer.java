package io.lolyay.discordmsend.server.music.consumers;

import io.lolyay.discordmsend.server.music.providers.HighQualityOpusStreamer;

public interface ITrackConsumer {
    void stop();
    void start();

    HighQualityOpusStreamer getStreamer();
}
