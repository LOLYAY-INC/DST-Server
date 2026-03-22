package io.lolyay.discordmsend.server;


import io.lolyay.discordmsend.network.types.ModdedInfo;
import io.lolyay.discordmsend.network.types.ServerFeatures;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ServerInitData {
    private String serverName;
    private String serverVersion;
    private ServerFeatures features;
    private String dapiVersion;
    private String countryCode;
    @Builder.Default
    private boolean singleGuildHQ = false;

}
