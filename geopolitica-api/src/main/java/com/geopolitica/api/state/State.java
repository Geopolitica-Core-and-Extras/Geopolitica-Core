package com.geopolitica.api.state;

import com.geopolitica.api.nation.Nation;
import com.geopolitica.api.town.Resident;
import com.geopolitica.api.town.Town;

import java.awt.Color;
import java.util.Collection;
import java.util.UUID;

/** A state: a group of towns within a nation. */
public interface State {

    UUID getUniqueId();

    String getName();

    void setName(String name);

    Nation getNation();

    Town getCapital();

    Resident getLeader();

    void setLeader(Resident leader);

    Collection<Town> getTowns();

    double getBankBalance();

    void depositBank(double amount);

    boolean withdrawBank(double amount);

    String getDescription();

    void setDescription(String description);

    Color getMapColor();

    void setMapColor(Color color);

    boolean isOpen();

    void setOpen(boolean open);

    boolean isFrozen();

    void setFrozen(boolean frozen);
}
