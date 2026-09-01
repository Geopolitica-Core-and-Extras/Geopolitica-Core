package com.geopolitica.api.events;

import com.geopolitica.api.nation.DiplomaticStatus;
import com.geopolitica.api.nation.Nation;
import org.bukkit.event.HandlerList;

/** Fired when the diplomatic status between two nations changes. */
public class NationRelationChangeEvent extends GeopoliticaEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Nation nationA;
    private final Nation nationB;
    private final DiplomaticStatus from;
    private final DiplomaticStatus to;

    public NationRelationChangeEvent(Nation nationA, Nation nationB, DiplomaticStatus from, DiplomaticStatus to) {
        this.nationA = nationA;
        this.nationB = nationB;
        this.from = from;
        this.to = to;
    }

    public Nation getNationA() {
        return nationA;
    }

    public Nation getNationB() {
        return nationB;
    }

    public DiplomaticStatus getFrom() {
        return from;
    }

    public DiplomaticStatus getTo() {
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
