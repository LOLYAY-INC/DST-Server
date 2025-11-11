package io.lolyay.discordmsend.server.yt;

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState;
import com.sedmelluq.discord.lavaplayer.track.TrackMarker;
import com.sedmelluq.discord.lavaplayer.track.info.AudioTrackInfoBuilder;
import org.checkerframework.checker.units.qual.A;

public class NoOpAudioTrackHelper implements AudioTrack {
    private final String uri;
    private long pos = 0;

    public NoOpAudioTrackHelper(String uri){
        this.uri = uri;
    }

    @Override
    public AudioTrackInfo getInfo() {
        return AudioTrackInfoBuilder.empty().setUri(uri).setIdentifier(uri).setAuthor("Unknown").setIsStream(false).setLength(0L).build();
    }

    @Override
    public String getIdentifier() {
        return uri;
    }

    @Override
    public AudioTrackState getState() {
        return AudioTrackState.PLAYING;
    }

    @Override
    public void stop() {
        //NO-OP
    }

    @Override
    public boolean isSeekable() {
        //NO-OP
        return true;
    }

    @Override
    public long getPosition() {
        return pos;
    }

    @Override
    public void setPosition(long l) {
        pos = l;
    }

    @Override
    public void setMarker(TrackMarker trackMarker) {
        //NO-OP

    }

    @Override
    public void addMarker(TrackMarker trackMarker) {
        //NO-OP

    }

    @Override
    public void removeMarker(TrackMarker trackMarker) {
        //NO-OP

    }

    @Override
    public long getDuration() {
        //NO-OP

        return 0;
    }

    @Override
    public AudioTrack makeClone() {
        //NO-OP
        return this;
    }

    @Override
    public AudioSourceManager getSourceManager() {
        //NO-OP
        return null;
    }

    @Override
    public void setUserData(Object o) {
        //NO-OP
    }

    @Override
    public Object getUserData() {
        //NO-OP
        return null;
    }

    @Override
    public <T> T getUserData(Class<T> aClass) {
        //NO-OP
        return null;
    }
}
