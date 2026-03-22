package io.lolyay.discordmsend.obj;

public enum EndReason {
    FINISHED(true),
    STOPPED(false),
    REPLACED(false);

    public final boolean mayStartNext;

    private EndReason(boolean mayStartNext) {
        this.mayStartNext = mayStartNext;
    }
}
