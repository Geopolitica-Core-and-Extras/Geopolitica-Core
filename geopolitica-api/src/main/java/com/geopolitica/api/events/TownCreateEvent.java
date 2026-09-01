package com.geopolitica.api.events;

import com.geopolitica.api.town.Resident;
import com.geopolitica.api.town.Town;
import org.bukkit.event.HandlerList;

/** Fired after a town is founded. */
public class TownCreateEvent extends GeopoliticaEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Town town;
    private final Resident owner;

    public TownCreateEvent(Town town, Resident owner) {
        this.town = town;
        this.owner = owner;
    }

    public Town getTown() {
        return town;
    }

    public Resident getOwner() {
        return owner;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
