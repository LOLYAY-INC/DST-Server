package io.lolyay.discordmsend.server.yt;

import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.YoutubeSourceOptions;
import dev.lavalink.youtube.clients.*;
import io.lolyay.discordmsend.obj.MusicTrack;
import io.lolyay.discordmsend.server.config.ConfigFile;
import io.lolyay.discordmsend.util.logging.Logger;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class YoutubeCipherH {
    private final YoutubeAudioSourceManager source;
    public YoutubeCipherH() { // we need this for search XD
        YoutubeSourceOptions options = new YoutubeSourceOptions()
                .setRemoteCipher(
                        ConfigFile.cipherServerURl,
                        ConfigFile.cipherServerApiKey,
                        "DCT Server (lolyay)")
                .setAllowSearch(true)
                .setAllowDirectPlaylistIds(true)
                .setAllowDirectVideoIds(true);

        source = new YoutubeAudioSourceManager(options, new MusicWithThumbnail(), new TvHtml5EmbeddedWithThumbnail(), new AndroidMusicWithThumbnail(), new WebWithThumbnail(), new WebEmbeddedWithThumbnail(), new MWebWithThumbnail(), new AndroidVrWithThumbnail());

        // Use OAuth2 if configured
        if (!ConfigFile.youtubeOauthRefreshToken.isEmpty()) {
            Logger.info("Using YouTube OAuth2 from config");
            source.useOauth2(ConfigFile.youtubeOauthRefreshToken, true);
        } else {
            Logger.warn("No YouTube OAuth2 refresh token configured. Some features may be limited.");
        }
    }

    public YoutubeAudioSourceManager getSource() {
        return source;
    }
}
