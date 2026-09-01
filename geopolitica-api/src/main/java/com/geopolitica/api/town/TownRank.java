package com.geopolitica.api.town;

import java.util.Set;

/** A named permission set that residents can be assigned within a town. */
public interface TownRank {

    String getName();

    Set<TownPermission> getPermissions();

    boolean hasPermission(TownPermission permission);

    void setPermission(TownPermission permission, boolean allowed);

    boolean isOwnerRank();

    boolean isDefaultRank();
}
