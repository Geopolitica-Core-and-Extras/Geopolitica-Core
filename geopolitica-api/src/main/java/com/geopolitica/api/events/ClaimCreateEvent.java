package com.geopolitica.api.events;

import com.geopolitica.api.claim.Claim;
import org.bukkit.event.HandlerList;

/** Fired after a town claims a chunk. */
public class ClaimCreateEvent extends GeopoliticaEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Claim claim;

    public ClaimCreateEvent(Claim claim) {
        this.claim = claim;
    }

    public Claim getClaim() {
        return claim;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
