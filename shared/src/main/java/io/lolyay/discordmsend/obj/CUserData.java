package io.lolyay.discordmsend.obj;


import io.lolyay.discordmsend.network.types.ClientFeatures;
/**
 * Represents user data sent by the client to the server during the login phase.
 *
 * @param userAgent The client's user agent (e.g. "Discord/12345")
 * @param userVersion The client's version (e.g. "1.0.0")
 * @param userAuthor The client's author (e.g. "LolyAI")
 * @param features The client's enabled features
 *
 * @see ClientFeatures
 */
public record CUserData(String userAgent, String userVersion, String userAuthor, ClientFeatures features) { }
