package io.lolyay.discordmsend.obj;

public enum EndReason {
    FINISHED(true),
    LOAD_FAILED(true),
    STOPPED(false),
    REPLACED(false),
    CLEANUP(false);

    public final boolean mayStartNext;

    private EndReason(boolean mayStartNext) {
        this.mayStartNext = mayStartNext;
    }
}
