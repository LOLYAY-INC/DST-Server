package io.lolyay.discordmsend.server;


import io.lolyay.discordmsend.network.types.ModdedInfo;
import io.lolyay.discordmsend.network.types.ServerFeatures;

public class ServerInitData {
    private  String serverName;
    private  String serverVersion;
    private ServerFeatures features;
    private  String ytSourceVersion;
    private  String countryCode;
    private ModdedInfo moddedInfo;
    private boolean singleGuildHQ = false;

    public String serverName() {
        return serverName;
    }

    public ServerInitData setServerName(String serverName) {
        this.serverName = serverName;
        return this;
    }

    public String serverVersion() {
        return serverVersion;
    }

    public ServerInitData setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
        return this;
    }

    public ServerFeatures features() {
        return features;
    }

    public ServerInitData setFeatures(ServerFeatures features) {
        this.features = features;
        return this;
    }

    public String ytSourceVersion() {
        return ytSourceVersion;
    }

    public ServerInitData setYtSourceVersion(String ytSourceVersion) {
        this.ytSourceVersion = ytSourceVersion;
        return this;
    }

    public String countryCode() {
        return countryCode;
    }

    public ServerInitData setCountryCode(String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    public ModdedInfo moddedInfo() {
        return moddedInfo;
    }

    public ServerInitData setModdedInfo(ModdedInfo moddedInfo) {
        this.moddedInfo = moddedInfo;
        return this;
    }

    public boolean singleGuildHQ() {
        return singleGuildHQ;
    }

    public ServerInitData setSingleGuildHQ(boolean singleGuildHQ) {
        this.singleGuildHQ = singleGuildHQ;
        return this;
    }
}
