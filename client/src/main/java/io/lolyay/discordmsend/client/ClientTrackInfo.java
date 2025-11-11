package io.lolyay.discordmsend.client;

public class ClientTrackInfo {
    public String trackName;
    public String trackAuthor;
    public String artworkUrl;
    public long duration;
    public int id;


    public ClientTrackInfo(String trackName, String trackAuthor, String artworkUrl, long duration, int id) {
        this.trackName = trackName;
        this.trackAuthor = trackAuthor;
        this.artworkUrl = artworkUrl;
        this.duration = duration;
        this.id = id;
    }
}
