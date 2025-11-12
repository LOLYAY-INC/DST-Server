package io.lolyay.discordmsend.server.music.consumers;

import io.lolyay.discordmsend.server.music.providers.HighQualityOpusStreamer;

public interface ITrackConsumer {
    void stop();
    void start();
    default void undestroy(){
        //This is for PacketTrackConsumer to start the threads again!
    }
    HighQualityOpusStreamer getStreamer();
}
