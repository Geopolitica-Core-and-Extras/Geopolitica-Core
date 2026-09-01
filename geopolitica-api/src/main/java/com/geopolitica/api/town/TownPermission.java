package com.geopolitica.api.town;

/** A privileged action within a town, granted per-rank. */
public enum TownPermission {
    CLAIM_LAND,
    UNCLAIM_LAND,
    INVITE_RESIDENT,
    KICK_RESIDENT,
    MANAGE_RANKS,
    ASSIGN_RANKS,
    DEPOSIT_BANK,
    WITHDRAW_BANK,
    SET_TAXES,
    EDIT_METADATA,
    MANAGE_PLOT_PERMISSIONS,
    SET_HOME,
    TOGGLE_PVP,
    DISBAND_TOWN,
    MANAGE_NATION
}
