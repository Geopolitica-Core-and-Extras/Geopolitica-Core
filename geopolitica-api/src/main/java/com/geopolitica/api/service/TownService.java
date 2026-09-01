package com.geopolitica.api.service;

import com.geopolitica.api.town.Resident;
import com.geopolitica.api.town.Town;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/** Entry point for reading and managing towns and residents. */
public interface TownService {

    Optional<Town> getTown(String name);

    Optional<Town> getTown(UUID townId);

    Collection<Town> getTowns();

    boolean isNameTaken(String name);

    Town createTown(String name, Resident owner);

    void disbandTown(Town town);

    Resident getResident(UUID playerId);

    Optional<Resident> getResident(String name);

    Collection<Resident> getResidents();
}
