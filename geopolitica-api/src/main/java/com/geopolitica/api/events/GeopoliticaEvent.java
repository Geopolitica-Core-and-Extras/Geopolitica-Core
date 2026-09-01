package com.geopolitica.api.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

/** Base class for every event Geopolitica fires. */
public abstract class GeopoliticaEvent extends Event implements Cancellable {

    private boolean cancelled;

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
