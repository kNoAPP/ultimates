package com.knoban.ultimates.player;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import com.knoban.atlas.data.local.JsonDataStore;
import com.knoban.ultimates.Ultimates;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * This class handles the loading, caching, and saving of {@link LocalPDStore} instances. Use this class to get
 * those instances in order to get or save data. It is not recommended to modify this class.
 *
 * This class is specifically designed for Ultimates, but could be moved to Atlas with a little work.
 *
 * @author Alden Bansemer (kNoAPP)
 */
public class LocalPDStoreManager {

    private final Ultimates plugin;
    private final File dataFolder;
    private final JsonDataStore dataStore;

    // Increase for better CPU. Decrease for better RAM.
    private static final int CACHE_FOR_SECONDS = 120;
    private final Cache<UUID, LocalPDStore> cachedPds = CacheBuilder.newBuilder()
            .removalListener(this::cacheExpiredSaveToFile).expireAfterAccess(CACHE_FOR_SECONDS, TimeUnit.SECONDS)
            .build();

    /**
     * Create a singleton data store for all local player data. This should be called once by the main plugin instance
     * and referenced from the main class.
     * @param plugin The main plugin instance (in this case, Ultimates)
     */
    public LocalPDStoreManager(@NotNull Ultimates plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        this.dataStore = new JsonDataStore(plugin);
        dataFolder.mkdirs();
    }

    /**
     * Retrieve a {@link Player}'s locally stored data. Use this call if you have access to a {@link Player} object
     * instance. Highly recommended to use this call primarily for online players as this method will update the
     * {@link LocalPDStore} with the {@link Player}'s name automatically before returning it.
     *
     * DO NOT STORE THE RETURNED INSTANCE! Always get the instance from the manager.
     *
     * @param player The player to get the local data of
     * @return The player's local data. If none exists, it will return a new instance of local data
     */
    @NotNull
    public LocalPDStore getPlayerDataStore(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        LocalPDStore pds = getPlayerDataStore(uuid);
        if(pds == null) {
            pds = new LocalPDStore(player);
            cachedPds.put(uuid, pds);
        }

        if(!pds.getName().equals(player.getName()))
            pds.setName(player.getName());

        return pds;
    }

    /**
     * Retrieve an offline player's locally stored data. This call should primarily be made for offline players.
     * While you may use it for online players too, it is recommended to use the {@link Player} object
     * because that call will update the {@link LocalPDStore} with the {@link Player}'s name before returning it.
     *
     * Additionally, this call may return null if a player's data cannot be found. This is unlike the other call which
     * will never return null since a proper {@link LocalPDStore} may be created.
     *
     * DO NOT STORE THE RETURNED INSTANCE! Always get the instance from the manager.
     *
     * @param uuid The uuid of the player to get the local data of
     * @return The player's local data. If none could be found, this returns null.
     */
    @Nullable
    public LocalPDStore getPlayerDataStore(@NotNull UUID uuid) {
        LocalPDStore pds = cachedPds.getIfPresent(uuid);
        if(pds == null) {
            cachedPds.cleanUp(); // Edge case, ensure our data was saved.

            File importFrom = new File(dataFolder, uuid + ".json");
            if(importFrom.exists())
                pds = dataStore.getJson(importFrom, LocalPDStore.class);

            if(pds != null)
                cachedPds.put(uuid, pds);
        }

        return pds;
    }

    /**
     * Immediately saves all cached data back to the local disk files. The cache is cleared so new
     * calls to the data store will retrieve data from the disk.
     */
    public void clearCacheAndSave() {
        cachedPds.invalidateAll();
        cachedPds.cleanUp();
    }

    /**
     * Helper method to automatically save data when removed from the cache. When data is removed, it is
     * saved to the disk files.
     * @param notification The notification from the cache.
     */
    private void cacheExpiredSaveToFile(@NotNull RemovalNotification<UUID, LocalPDStore> notification) {
        UUID uuid = notification.getKey();
        LocalPDStore data = notification.getValue();

        File toSave = new File(dataFolder, uuid + ".json");
        dataStore.saveJson(toSave, data);
    }
}