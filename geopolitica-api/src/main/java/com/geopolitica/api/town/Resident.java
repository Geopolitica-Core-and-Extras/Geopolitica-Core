package com.geopolitica.api.town;

import org.bukkit.OfflinePlayer;

import java.util.Optional;
import java.util.UUID;

/** A player known to Geopolitica, whether or not they currently belong to a town. */
public interface Resident {

    UUID getUniqueId();

    OfflinePlayer getOfflinePlayer();

    String getName();

    Optional<Town> getTown();

    Optional<TownRank> getRank();

    boolean hasPermission(TownPermission permission);

    long getLastSeen();
}
