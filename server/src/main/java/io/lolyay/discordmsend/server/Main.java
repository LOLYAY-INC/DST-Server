package io.lolyay.discordmsend.server;

import io.lolyay.discordmsend.network.Enviroment;
import io.lolyay.discordmsend.network.protocol.packet.PacketRegistry;
import io.lolyay.discordmsend.network.types.ModdedInfo;
import io.lolyay.discordmsend.network.types.ServerFeatures;
import io.lolyay.discordmsend.server.config.ConfigFile;
import io.lolyay.discordmsend.util.logging.Logger;

import java.io.IOException;
import java.io.ObjectInputFilter;

public class Main {
    public static final PacketRegistry packetRegistry = new PacketRegistry();
    private static Server server;

    private static ServerInitData serverInitData;

    public static void main(String[] args) throws Exception {
        packetRegistry.registerAll();
        ConfigFile.load();
        Logger.DEBUG = ConfigFile.debug;
        
        // Initialize server with config values
        serverInitData = new ServerInitData()
                .setServerName("Default DST Server")
                .setServerVersion("Alpha-5.0.0-P" + Enviroment.PROTOCOL_VERSION)
                .setCountryCode(ConfigFile.countryCode)
                .setFeatures(new ServerFeatures(
                        ServerFeatures.Feature.CAN_DO_YOUTUBE,
                        ServerFeatures.Feature.CAN_E2EE,
                        ServerFeatures.Feature.CAN_OPUS,
                        ServerFeatures.Feature.DOES_KEEP_ALIVE,
                        ServerFeatures.Feature.IS_DISCORD_ALLOWED,
                        ServerFeatures.Feature.SUPPORTS_SEEKING,
                        ServerFeatures.Feature.USES_MEDIA
                )).setModdedInfo(new ModdedInfo())
                .setYtSourceVersion("1.15.0")
                .setSingleGuildHQ(ConfigFile.singleGuildHQ);
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Logger.info("Shutting down...");
            try {
                ConfigFile.save();
            } catch (IOException e) {
                Logger.err("Error saving Config: ");
                throw new RuntimeException(e);
            }
        }));
        server = new Server(2677, Enviroment.PROTOCOL_VERSION, packetRegistry, serverInitData, ConfigFile.apiKey);
        Logger.log("Starting Server with protocol version " + Enviroment.PROTOCOL_VERSION + " on port " + 2677);
        server.start();

    }
}
