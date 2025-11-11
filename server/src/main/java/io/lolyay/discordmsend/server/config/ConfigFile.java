package io.lolyay.discordmsend.server.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ConfigFile {

    // ==== Fields ====
    public static boolean enableTrackCache = true;
    public static boolean allowDownload = true;
    public static boolean singleGuildHQ = false;
    public static boolean debug = false;

    public static String trackUploadBucketUrl = "";
    public static String publicDownloadBucketUrl = "";
    public static String s3AccessKey = "";
    public static String s3SecretKey = "";
    public static String s3Region = "eu-central-1";

    public static String cipherServerURl = "";
    public static String cipherServerApiKey = "";
    public static String youtubeOauthRefreshToken = "";

    public static String countryCode = "US";
    public static String apiKey = "";

    private static final Path CONFIG_PATH = Path.of("config.yml");
    private static final YamlConfigurationLoader LOADER = YamlConfigurationLoader.builder()
            .path(CONFIG_PATH)
            .indent(2)
            .nodeStyle(NodeStyle.BLOCK)
            .defaultOptions(opts -> opts.shouldCopyDefaults(true))
            .build();

    // ==== Public API ====

    public static void load() throws IOException {
        if (!Files.exists(CONFIG_PATH)) {
            saveDefaults();
        }

        CommentedConfigurationNode root = LOADER.load();

        enableTrackCache = root.node("enableTrackCache").getBoolean(enableTrackCache);
        allowDownload = root.node("allowDownload").getBoolean(allowDownload);
        singleGuildHQ = root.node("singleGuildHQ").getBoolean(singleGuildHQ);
        debug = root.node("debug").getBoolean(debug);

        trackUploadBucketUrl = root.node("trackUploadBucketUrl").getString(trackUploadBucketUrl);
        publicDownloadBucketUrl = root.node("publicDownloadBucketUrl").getString(publicDownloadBucketUrl);
        s3AccessKey = root.node("s3AccessKey").getString(s3AccessKey);
        s3SecretKey = root.node("s3SecretKey").getString(s3SecretKey);
        s3Region = root.node("s3Region").getString(s3Region);

        cipherServerURl = root.node("cipherServerURl").getString(cipherServerURl);
        cipherServerApiKey = root.node("cipherServerApiKey").getString(cipherServerApiKey);
        youtubeOauthRefreshToken = root.node("youtubeOauthRefreshToken").getString(youtubeOauthRefreshToken);

        countryCode = root.node("countryCode").getString(countryCode);
        apiKey = root.node("apiKey").getString(countryCode);
    }

    /** Saves current static field values back to the config.yml */
    public static void save() throws IOException {
        // Read existing config to preserve any manual edits
        if (!Files.exists(CONFIG_PATH)) {
            saveDefaults();
            return;
        }
        
        // Load and update values
        CommentedConfigurationNode root = LOADER.load();
        
        root.node("enableTrackCache").set(enableTrackCache);
        root.node("allowDownload").set(allowDownload);
        root.node("singleGuildHQ").set(singleGuildHQ);
        root.node("debug").set(debug);
        
        root.node("trackUploadBucketUrl").set(trackUploadBucketUrl);
        root.node("publicDownloadBucketUrl").set(publicDownloadBucketUrl);
        root.node("s3AccessKey").set(s3AccessKey);
        root.node("s3SecretKey").set(s3SecretKey);
        root.node("s3Region").set(s3Region);
        
        root.node("cipherServerURl").set(cipherServerURl);
        root.node("cipherServerApiKey").set(cipherServerApiKey);
        root.node("youtubeOauthRefreshToken").set(youtubeOauthRefreshToken);
        
        root.node("countryCode").set(countryCode);
        root.node("apiKey").set(apiKey);

        LOADER.save(root);
    }

    // ==== Private helpers ====

    private static void saveDefaults() throws IOException {
        // Copy default config from resources
        try (InputStream in = ConfigFile.class.getResourceAsStream("/default-config.yml")) {
            if (in == null) {
                throw new IOException("Could not find default-config.yml in resources!");
            }
            Files.copy(in, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void setDefaults(CommentedConfigurationNode root) throws SerializationException {
        // Not used anymore - we write manually for comments
    }
}
