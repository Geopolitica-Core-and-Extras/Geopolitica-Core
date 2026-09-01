package com.geopolitica.api.events;

import com.geopolitica.api.state.State;
import com.geopolitica.api.town.Town;
import org.bukkit.event.HandlerList;

/** Fired when a town's state membership changes. Either side may be null. */
public class TownStateChangeEvent extends GeopoliticaEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Town town;
    private final State from;
    private final State to;

    public TownStateChangeEvent(Town town, State from, State to) {
        this.town = town;
        this.from = from;
        this.to = to;
    }

    public Town getTown() {
        return town;
    }

    public State getFrom() {
        return from;
    }

    public State getTo() {
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
