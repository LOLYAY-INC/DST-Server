package io.lolyay.discordmsend.obj;


import io.lolyay.discordmsend.network.types.ClientFeatures;

public record CUserData(String userAgent, String userVersion, String userAuthor, ClientFeatures features) { }
