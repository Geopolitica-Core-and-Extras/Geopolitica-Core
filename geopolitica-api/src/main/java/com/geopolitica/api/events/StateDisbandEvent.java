package com.geopolitica.api.events;

import com.geopolitica.api.state.State;
import org.bukkit.event.HandlerList;

/** Fired before a state is disbanded. */
public class StateDisbandEvent extends GeopoliticaEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final State state;

    public StateDisbandEvent(State state) {
        this.state = state;
    }

    public State getState() {
        return state;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
