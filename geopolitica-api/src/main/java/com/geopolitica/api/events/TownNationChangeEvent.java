package com.geopolitica.api.events;

import com.geopolitica.api.nation.Nation;
import com.geopolitica.api.town.Town;
import org.bukkit.event.HandlerList;

/** Fired when a town's direct nation membership changes. Either side may be null. */
public class TownNationChangeEvent extends GeopoliticaEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Town town;
    private final Nation from;
    private final Nation to;

    public TownNationChangeEvent(Town town, Nation from, Nation to) {
        this.town = town;
        this.from = from;
        this.to = to;
    }

    public Town getTown() {
        return town;
    }

    public Nation getFrom() {
        return from;
    }

    public Nation getTo() {
        return to;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
