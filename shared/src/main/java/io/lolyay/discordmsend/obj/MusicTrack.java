package io.lolyay.discordmsend.obj;

public class MusicTrack {
    private final int trackId;

    private MusicTrack(int id){
        this.trackId = id;
    }

    public static MusicTrack ofId(int id){
        return new MusicTrack(id);
    }

    public int getTrackId() {
        return trackId;
    }
}
