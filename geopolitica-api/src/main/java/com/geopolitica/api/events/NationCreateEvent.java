package com.geopolitica.api.events;

import com.geopolitica.api.nation.Nation;
import com.geopolitica.api.town.Town;
import org.bukkit.event.HandlerList;

/** Fired after a nation is founded. */
public class NationCreateEvent extends GeopoliticaEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Nation nation;
    private final Town capital;

    public NationCreateEvent(Nation nation, Town capital) {
        this.nation = nation;
        this.capital = capital;
    }

    public Nation getNation() {
        return nation;
    }

    public Town getCapital() {
        return capital;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
