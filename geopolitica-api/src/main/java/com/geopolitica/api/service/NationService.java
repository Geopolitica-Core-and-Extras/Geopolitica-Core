package com.geopolitica.api.service;

import com.geopolitica.api.nation.DiplomaticStatus;
import com.geopolitica.api.nation.Nation;
import com.geopolitica.api.state.State;
import com.geopolitica.api.town.Resident;
import com.geopolitica.api.town.Town;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/** Entry point for reading and managing nations, states, and inter-nation diplomacy. */
public interface NationService {

    Optional<Nation> getNation(String name);

    Optional<Nation> getNation(UUID nationId);

    Collection<Nation> getNations();

    boolean isNationNameTaken(String name);

    Nation createNation(String name, Town capital);

    void disbandNation(Nation nation);

    Optional<State> getState(String name);

    Optional<State> getState(UUID stateId);

    Collection<State> getStates();

    boolean isStateNameTaken(String name);

    State createState(String name, Nation nation, Town capital);

    void disbandState(State state);

    Nation secedeState(State state, String newNationName);

    void setStateCapital(State state, Town newCapital);

    void setStateLeader(State state, Resident leader);

    void kickTownFromState(State state, Town town);

    void collectStateTax(State state, Town town, double amount);

    void joinNation(Town town, Nation nation);

    void leaveNation(Town town);

    void joinState(Town town, State state);

    void leaveState(Town town);

    DiplomaticStatus getRelation(Nation a, Nation b);

    void declareWar(Nation a, Nation b);

    void declarePeace(Nation a, Nation b);

    void requestAlliance(Nation a, Nation b);

    void breakAlliance(Nation a, Nation b);
}
