package com.geopolitica.api.claim;

import com.geopolitica.api.town.Town;

/** A single claimed chunk. */
public interface Claim {

    Town getTown();

    String getWorldName();

    int getChunkX();

    int getChunkZ();

    String getPlotName();

    boolean isPermissionSet(TrustLevel trustLevel, ClaimPermission permission);

    void setPermission(TrustLevel trustLevel, ClaimPermission permission, boolean allowed);
}
