package com.geopolitica.core.economy;

import com.geopolitica.api.claim.Claim;
import com.geopolitica.api.town.Town;
import com.geopolitica.core.config.ConfigManager;
import com.geopolitica.core.model.TownImpl;
import com.geopolitica.core.service.ClaimServiceImpl;
import com.geopolitica.core.service.TownServiceImpl;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Charges each town daily upkeep (per {@code town.upkeep-per-claim-per-day}) from its bank,
 * applying {@code town.upkeep-failure-action} to towns that can't afford it. Runs on the main
 * thread: the service layer's maps aren't synchronized, so mutating town/claim state has to
 * happen on the same thread every other command/listener uses.
 */
public class UpkeepTask implements Runnable {

    private final JavaPlugin plugin;
    private final TownServiceImpl townService;
    private final ClaimServiceImpl claimService;
    private final ConfigManager configManager;
    private final EconomyHook economyHook;

    public UpkeepTask(JavaPlugin plugin, TownServiceImpl townService, ClaimServiceImpl claimService,
                       ConfigManager configManager, EconomyHook economyHook) {
        this.plugin = plugin;
        this.townService = townService;
        this.claimService = claimService;
        this.configManager = configManager;
        this.economyHook = economyHook;
    }

    @Override
    public void run() {
        double ratePerClaim = configManager.getUpkeepPerClaimPerDay();
        if (ratePerClaim <= 0 || !economyHook.isEnabled()) {
            return;
        }
        String failureAction = configManager.getUpkeepFailureAction().toUpperCase(Locale.ROOT);

        for (Town town : townService.getTowns()) {
            if (town.getClaimCount() <= 0) {
                continue;
            }
            double cost = town.getClaimCount() * ratePerClaim;
            if (town.withdrawBank(cost)) {
                if ("FREEZE".equals(failureAction) && town.isFrozen()) {
                    // An upkeep-driven freeze is meant to be a recoverable penalty, so lift it
                    // once the town has caught up - otherwise a single missed payment would
                    // freeze a town forever even after it pays. (This can't distinguish an
                    // upkeep freeze from one an admin set by hand for an unrelated reason,
                    // since there's only the one frozen flag; an admin who wants a freeze to
                    // stick regardless of payment will need to re-apply it.)
                    town.setFrozen(false);
                }
                saveTown(town);
                continue;
            }
            switch (failureAction) {
                case "RANDOM_UNCLAIM" -> randomUnclaim(town);
                case "FREEZE" -> {
                    town.setFrozen(true);
                    saveTown(town);
                }
                default -> {
                    // WARN, and any unrecognized value: leave the town as-is, just notify.
                }
            }
            warnOwner(town, cost);
        }
    }

    private void randomUnclaim(Town town) {
        List<Claim> claims = List.copyOf(town.getClaims());
        if (claims.isEmpty()) {
            return;
        }
        Claim toDrop = claims.get(ThreadLocalRandom.current().nextInt(claims.size()));
        claimService.forceUnclaim(toDrop);
    }

    private void warnOwner(Town town, double cost) {
        Player owner = Bukkit.getPlayer(town.getOwner().getUniqueId());
        if (owner != null) {
            owner.sendMessage(ChatColor.GOLD + "[Geopolitica] " + ChatColor.RED
                    + town.getName() + " could not pay upkeep (" + ChatColor.RESET
                    + economyHook.format(cost) + ChatColor.RED + ").");
        }
    }

    private void saveTown(Town town) {
        if (town instanceof TownImpl impl) {
            townService.saveTown(impl);
        } else {
            plugin.getLogger().warning("Unknown Town implementation during upkeep: " + town.getClass());
        }
    }
}
