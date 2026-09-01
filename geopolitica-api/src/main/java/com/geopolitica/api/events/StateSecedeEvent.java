package com.geopolitica.api.events;

import com.geopolitica.api.nation.Nation;
import com.geopolitica.api.state.State;
import org.bukkit.event.HandlerList;

/** Fired before a state secedes from its nation to form a new, independent nation. */
public class StateSecedeEvent extends GeopoliticaEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final State formerState;
    private final Nation formerNation;
    private final String newNationName;

    public StateSecedeEvent(State formerState, Nation formerNation, String newNationName) {
        this.formerState = formerState;
        this.formerNation = formerNation;
        this.newNationName = newNationName;
    }

    public State getFormerState() {
        return formerState;
    }

    public Nation getFormerNation() {
        return formerNation;
    }

    public String getNewNationName() {
        return newNationName;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
