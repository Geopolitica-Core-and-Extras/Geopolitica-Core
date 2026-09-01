package com.geopolitica.api.events;

import com.geopolitica.api.claim.Claim;
import org.bukkit.event.HandlerList;

/** Fired before a claimed chunk is unclaimed. */
public class ClaimRemoveEvent extends GeopoliticaEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Claim claim;

    public ClaimRemoveEvent(Claim claim) {
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
