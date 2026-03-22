package io.lolyay.discordmsend.client;

public record ServerStatus(long maxMem, long freeMem, int cpuUsageP, int players, int clients) {
}
