package com.geopolitica.api;

import com.geopolitica.api.service.ClaimService;
import com.geopolitica.api.service.NationService;
import com.geopolitica.api.service.TownService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Static convenience accessors for Geopolitica's services, registered via the Bukkit services manager. */
public final class GeopoliticaAPI {

    private GeopoliticaAPI() {
    }

    public static TownService getTownService() {
        return load(TownService.class);
    }

    public static ClaimService getClaimService() {
        return load(ClaimService.class);
    }

    public static NationService getNationService() {
        return load(NationService.class);
    }

    private static <T> T load(Class<T> type) {
        RegisteredServiceProvider<T> provider = Bukkit.getServicesManager().getRegistration(type);
        if (provider == null) {
            throw new IllegalStateException("Geopolitica's " + type.getSimpleName() + " is not registered; is the plugin enabled?");
        }
        return provider.getProvider();
    }
}
