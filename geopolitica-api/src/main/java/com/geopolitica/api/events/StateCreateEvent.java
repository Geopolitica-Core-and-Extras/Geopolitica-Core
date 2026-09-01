package com.geopolitica.api.events;

import com.geopolitica.api.nation.Nation;
import com.geopolitica.api.state.State;
import com.geopolitica.api.town.Town;
import org.bukkit.event.HandlerList;

/** Fired after a state is founded within a nation. */
public class StateCreateEvent extends GeopoliticaEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final State state;
    private final Nation nation;
    private final Town capital;

    public StateCreateEvent(State state, Nation nation, Town capital) {
        this.state = state;
        this.nation = nation;
        this.capital = capital;
    }

    public State getState() {
        return state;
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
