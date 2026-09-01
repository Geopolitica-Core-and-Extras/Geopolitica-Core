package com.geopolitica.api.events;

import com.geopolitica.api.town.Resident;
import com.geopolitica.api.town.Town;
import org.bukkit.event.HandlerList;

/** Fired after a resident joins a town. */
public class ResidentJoinTownEvent extends GeopoliticaEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Resident resident;
    private final Town town;

    public ResidentJoinTownEvent(Resident resident, Town town) {
        this.resident = resident;
        this.town = town;
    }

    public Resident getResident() {
        return resident;
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
