package com.geopolitica.api.town;

import com.geopolitica.api.claim.Claim;
import com.geopolitica.api.nation.Nation;
import com.geopolitica.api.state.State;
import org.bukkit.Location;

import java.awt.Color;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/** A town: the base unit of Geopolitica's political hierarchy. */
public interface Town {

    UUID getUniqueId();

    String getName();

    void setName(String name);

    Resident getOwner();

    Collection<Resident> getResidents();

    Collection<TownRank> getRanks();

    Optional<TownRank> getRank(String rankName);

    TownRank createRank(String rankName);

    void deleteRank(String rankName);

    Optional<State> getState();

    Optional<Nation> getNation();

    Collection<Claim> getClaims();

    int getClaimCount();

    int getMaxClaims();

    double getBankBalance();

    void depositBank(double amount);

    boolean withdrawBank(double amount);

    String getDescription();

    void setDescription(String description);

    Color getMapColor();

    void setMapColor(Color color);

    Optional<Location> getHomeLocation();

    void setHomeLocation(Location location);

    boolean isOpen();

    void setOpen(boolean open);

    boolean isPvpEnabled();

    void setPvpEnabled(boolean enabled);

    boolean isFrozen();

    void setFrozen(boolean frozen);
}
