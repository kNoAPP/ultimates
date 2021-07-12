package com.knoban.ultimates.player;

import com.knoban.atlas.world.Coordinate;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Use this class to save any {@link org.bukkit.entity.Player} data that does not need to be stored remotely. Data in
 * this class is saved locally.
 *
 * All private member fields are automatically handled and saved. Expand this class to include all data you need to
 * save. This class should intentionally be a little messy. We don't want to have like 9 folders of player data
 * for a single player. Just one .json file per player will do.
 *
 * This class differs from the CardHolder class in that CardHolder contains data to be stored remotely in SQL. Data
 * here shouldn't need to be synced across servers.
 *
 * @author Alden Bansemer (kNoAPP)
 */
public class LocalPDStore {

    /**
     * JOURNAL FOR PARSING:
     * 1. Location does not parse with Gson. Use {@link Coordinate} instead.
     *
     * Add more here as you find more information.
     */

    private UUID uuid;
    private String name;

    private Coordinate recallLocation;
    private int freeRespawns;

    /**
     * For Gson parsing only! DO NOT USE!
     */
    public LocalPDStore() {}

    protected LocalPDStore(@NotNull Player player, int freeRespawns) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();

        this.freeRespawns = freeRespawns;
    }

    @NotNull
    public UUID getUniqueId() {
        return uuid;
    }

    @NotNull
    public String getName() {
        return name;
    }

    /**
     * This should not be called by anything except the {@link LocalPDStore}
     * @param name The name of the Player
     */
    protected void setName(@NotNull String name) {
        this.name = name;
    }

    @Nullable
    public Coordinate getRecallLocation() {
        return recallLocation;
    }

    public void setRecallLocation(@Nullable Coordinate recallLocation) {
        this.recallLocation = recallLocation;
    }

    public int getFreeRespawns() {
        return freeRespawns;
    }

    public void setFreeRespawns(int freeRespawns) {
        this.freeRespawns = freeRespawns;
    }
}