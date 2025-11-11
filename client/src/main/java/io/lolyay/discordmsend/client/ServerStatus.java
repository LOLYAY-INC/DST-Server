package io.lolyay.discordmsend.client;

public class ServerStatus {
    private final long maxMem;
    private final long freeMem;
    private final int cpuUsageP;
    private final int players;
    private final int clients;

    public ServerStatus(long maxMem, long freeMem, int cpuUsageP, int players, int clients) {
        this.maxMem = maxMem;
        this.freeMem = freeMem;
        this.cpuUsageP = cpuUsageP;
        this.players = players;
        this.clients = clients;
    }

    public long getMaxMem() {
        return maxMem;
    }

    public long getFreeMem() {
        return freeMem;
    }

    public int getCpuUsageP() {
        return cpuUsageP;
    }

    public int getPlayers() {
        return players;
    }

    public int getClients() {
        return clients;
    }
}
