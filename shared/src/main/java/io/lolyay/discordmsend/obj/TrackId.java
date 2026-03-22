package io.lolyay.discordmsend.obj;

import lombok.Getter;

@Getter
public class TrackId {
    private final int trackId;

    private TrackId(int id){
        this.trackId = id;
    }

    public static TrackId ofId(int id){
        return new TrackId(id);
    }

}
