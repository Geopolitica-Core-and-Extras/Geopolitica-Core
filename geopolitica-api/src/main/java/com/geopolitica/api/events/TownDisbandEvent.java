package com.geopolitica.api.events;

import com.geopolitica.api.town.Town;
import org.bukkit.event.HandlerList;

/** Fired before a town is disbanded. */
public class TownDisbandEvent extends GeopoliticaEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Town town;

    public TownDisbandEvent(Town town) {
        this.town = town;
    }

    public Town getTown() {
        return town;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
