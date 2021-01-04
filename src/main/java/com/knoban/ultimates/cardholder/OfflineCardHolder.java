package com.knoban.ultimates.cardholder;

import com.knoban.atlas.callbacks.GenericCallback2;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OfflineCardHolder extends Holder {

    private static final ConcurrentHashMap<OfflineCardHolder, List<GenericCallback2<Boolean, OfflineCardHolder>>> enqueued = new ConcurrentHashMap<>();

    private final OfflinePlayer offlinePlayer;

    /**
     * Create a OfflineCardHolder to get data from a player.
     * @param plugin - Ultimates plugin instance
     * @param uuid - UUID of Player to create OfflineCardHolder from
     * @param name - Name of Player to create OfflineCardHolder from
     */
    private OfflineCardHolder(@NotNull Ultimates plugin, @NotNull UUID uuid, @NotNull String name) {
        super(plugin, uuid, name);
        this.offlinePlayer = Bukkit.getOfflinePlayer(uuid);
    }

    /**
     * @return the OfflinePlayer this OfflineCardHolder is tied to
     */
    @NotNull
    public OfflinePlayer getOfflinePlayer() {
        return offlinePlayer;
    }

    /**
     * Get a OfflineCardHolder instance given a Player instance
     * OfflineCardHolders are not cached, so you may call this as many times as needed
     * <br /><br />
     * DO NOT STORE OfflineCardHolders! They are unloaded after the callback is used.
     * Callback is run on its the main Bukkit game thread. This object should only be used for DB read/write.
     * <br /><br />
     * This call does not use a mutex and will not save changes. Read only!
     *
     * @param plugin - An instance of the Ultimates plugin
     * @param name - The name of the player to get the OfflineCardHolder instance from
     * @param callback - A callback with a boolean indicating a successful creation of {@link OfflineCardHolder}.
     */
    public static void getOfflineCardHolder(@NotNull Ultimates plugin, @NotNull String name,
                                            @NotNull GenericCallback2<Boolean, OfflineCardHolder> callback) {
        Thread t = new Thread(() -> {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
            OfflineCardHolder holder = new OfflineCardHolder(plugin, offlinePlayer.getUniqueId(),
                    offlinePlayer.getName());
            holder.loadWithoutMutex((success, toDraw) -> {
                if(success) {
                    toDraw.stream().filter(Card::isEnabled).forEach(holder::drawCards);

                    callback.call(true, holder);
                } else
                    callback.call(false, null);
            });
        });
        t.start();
    }

    /**
     * Get a OfflineCardHolder instance given a Player instance
     * OfflineCardHolders are not cached, so you may call this as many times as needed
     * <br /><br />
     * DO NOT STORE OfflineCardHolders! They are unloaded after the callback is used.
     * Callback is run on its the main Bukkit game thread. This object should only be used for DB read/write.
     * <br /><br />
     * This call uses a mutex. By using a mutex, you can ensure data consistency when writing.
     *
     * @param plugin - An instance of the Ultimates plugin
     * @param name - The name of the player to get the OfflineCardHolder instance from
     * @param timeout - The time in millis-seconds to wait for execution of this task before failing. Pass -1 for no
     * limit.
     * @param callback - A callback with a boolean indicating a successful creation of {@link OfflineCardHolder}.
     */
    public static void getOfflineCardHolder(@NotNull Ultimates plugin, @NotNull String name, long timeout,
                                            @NotNull GenericCallback2<Boolean, OfflineCardHolder> callback) {
        Thread t = new Thread(() -> {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
            OfflineCardHolder holder = new OfflineCardHolder(plugin, offlinePlayer.getUniqueId(),
                    offlinePlayer.getName());

            List<GenericCallback2<Boolean, OfflineCardHolder>> callbacks = enqueued.get(holder);
            if(callbacks == null) {
                callbacks = new ArrayList<>();
                enqueued.put(holder, callbacks);
                holder.load(timeout, false, (success, toDraw) -> {
                    List<GenericCallback2<Boolean, OfflineCardHolder>> callThese = enqueued.remove(holder);
                    if(success)
                        toDraw.stream().filter(Card::isEnabled).forEach(holder::drawCards);
                    callThese.forEach(c -> c.call(success, holder));
                    holder.save(false, null);
                });
            }
            callbacks.add(callback);
        });
        t.start();
    }

    public static void safeShutdown(Ultimates plugin) {
        for(OfflineCardHolder loading : enqueued.keySet()) {
            plugin.getLogger().warning("Tasks were forcibly removed for " + loading.getName() + " (" + loading.getUniqueId() + ")!");
            loading.removeMutex(true);
        }
    }
}
