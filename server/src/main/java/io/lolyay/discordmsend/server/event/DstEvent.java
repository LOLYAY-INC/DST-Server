package io.lolyay.discordmsend.server.event;

import io.lolyay.discordmsend.server.DstServer;
import io.lolyay.eventbus.CancellableEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public abstract class DstEvent extends CancellableEvent {
    private final DstServer server;
}
