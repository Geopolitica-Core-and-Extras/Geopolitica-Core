package com.geopolitica.api.service;

import com.geopolitica.api.claim.Claim;
import com.geopolitica.api.claim.ClaimPermission;
import com.geopolitica.api.town.Town;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;

/** Entry point for reading and managing claimed chunks. */
public interface ClaimService {

    Optional<Claim> getClaim(Location location);

    Optional<Claim> getClaim(Chunk chunk);

    Optional<Claim> getClaim(String worldName, int chunkX, int chunkZ);

    boolean isClaimed(Chunk chunk);

    Collection<Claim> getClaims(Town town);

    Claim claim(Town town, Chunk chunk);

    void unclaim(Claim claim);

    boolean hasPermission(Player player, Location location, ClaimPermission permission);
}
