package io.lolyay.discordmsend.network;

public enum Enviroment {
    CLIENT,
    SERVER;

    public static final int PROTOCOL_VERSION = 110;
    // yes, this has been increased by 1 every time protocol changes
}
