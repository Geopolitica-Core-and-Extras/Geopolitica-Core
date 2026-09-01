package com.geopolitica.api.events;

import com.geopolitica.api.nation.Nation;
import org.bukkit.event.HandlerList;

/** Fired before a nation is disbanded. */
public class NationDisbandEvent extends GeopoliticaEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Nation nation;

    public NationDisbandEvent(Nation nation) {
        this.nation = nation;
    }

    public Nation getNation() {
        return nation;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
