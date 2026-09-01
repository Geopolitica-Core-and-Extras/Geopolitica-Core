package com.geopolitica.api.events;

import com.geopolitica.api.town.Resident;
import com.geopolitica.api.town.Town;
import org.bukkit.event.HandlerList;

/** Fired before a resident leaves, is kicked from, or loses a town to disbandment. */
public class ResidentLeaveTownEvent extends GeopoliticaEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Resident resident;
    private final Town town;
    private final Cause cause;

    public ResidentLeaveTownEvent(Resident resident, Town town, Cause cause) {
        this.resident = resident;
        this.town = town;
        this.cause = cause;
    }

    public Resident getResident() {
        return resident;
    }

    public Town getTown() {
        return town;
    }

    public Cause getCause() {
        return cause;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /** Why a resident is leaving a town. */
    public enum Cause {
        LEFT,
        KICKED,
        TOWN_DISBANDED
    }
}
