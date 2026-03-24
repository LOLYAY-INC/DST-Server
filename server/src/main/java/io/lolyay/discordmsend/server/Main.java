package io.lolyay.discordmsend.server;

import io.lolyay.discordmsend.network.Enviroment;
import io.lolyay.discordmsend.network.protocol.packet.PacketRegistry;
import io.lolyay.discordmsend.network.types.ServerFeatures;
import io.lolyay.discordmsend.server.config.ConfigFile;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class Main {
    public static final PacketRegistry packetRegistry = new PacketRegistry();
    private static DstServer dstServer;

    private static ServerInitData serverInitData;

    public static void main(String[] args) throws Exception {
        packetRegistry.registerAll();
        ConfigFile.load();

        serverInitData = ServerInitData.builder()
                .serverName("Default DST Server")
                .serverVersion("Alpha-9.0.3-P" + Enviroment.PROTOCOL_VERSION)
                .countryCode("US")
                .features(new ServerFeatures(
                        ServerFeatures.Feature.CAN_DO_YOUTUBE,
                        ServerFeatures.Feature.CAN_E2EE,
                        ServerFeatures.Feature.CAN_OPUS,
                        ServerFeatures.Feature.DOES_KEEP_ALIVE,
                        ServerFeatures.Feature.IS_DISCORD_ALLOWED,
                        ServerFeatures.Feature.SUPPORTS_SEEKING,
                        ServerFeatures.Feature.USES_MEDIA
                ))
                .serverId("3.1.8")
                .singleGuildHQ(ConfigFile.singleGuildHQ)
                .build();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            try {
                ConfigFile.save();
            } catch (IOException e) {
                log.error("Error saving Config: ");
                throw new RuntimeException(e);
            }
        }));
        dstServer = new DstServer(2677, Enviroment.PROTOCOL_VERSION, packetRegistry, serverInitData, ConfigFile.apiKey);
        log.error("Starting Server with protocol version " + Enviroment.PROTOCOL_VERSION + " on port " + 2677);
        dstServer.start();

    }
}
