package io.lolyay.discordmsend.server.music.consumers;

import io.lolyay.discordmsend.server.music.providers.HighQualityOpusStreamer;

public interface ITrackConsumer {
    void stop();
    void start();
    //TODO make this into an interface in preperation to plugin api
    HighQualityOpusStreamer getStreamer();
}
