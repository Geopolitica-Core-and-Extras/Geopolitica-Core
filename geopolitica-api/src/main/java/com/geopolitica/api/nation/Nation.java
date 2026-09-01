package com.geopolitica.api.nation;

import com.geopolitica.api.state.State;
import com.geopolitica.api.town.Resident;
import com.geopolitica.api.town.Town;

import java.awt.Color;
import java.util.Collection;
import java.util.UUID;

/** A nation: a group of states and/or towns. */
public interface Nation {

    UUID getUniqueId();

    String getName();

    void setName(String name);

    Town getCapital();

    Resident getLeader();

    Collection<Town> getDirectTowns();

    Collection<State> getStates();

    Collection<Town> getAllTowns();

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

    DiplomaticStatus getRelation(Nation other);

    Collection<Nation> getAllies();

    Collection<Nation> getEnemies();
}
