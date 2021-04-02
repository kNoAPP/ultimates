package com.knoban.ultimates.missions.impl;

import com.knoban.atlas.utils.Tools;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.missions.Mission;
import com.knoban.ultimates.missions.MissionInfo;
import com.knoban.ultimates.missions.bossbar.BossBarConstant;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@MissionInfo(name = "destination")
public class DestinationMission extends Mission {

    private final Location destination;
    private final int range;

    private BukkitTask task;
    private final HashMap<UUID, TraversalTracker> traversal = new HashMap<>();

    public DestinationMission(@NotNull Ultimates plugin, @NotNull String uuid, @NotNull Map<String, Object> missionData) {
        super(plugin, uuid, missionData);
        this.maxProgress = 1000L;

        String worldName = (String) extraData.getOrDefault("world", "world");
        int x = ((Long) extraData.getOrDefault("x", 0L)).intValue();
        int y = ((Long) extraData.getOrDefault("y", 0L)).intValue();
        int z = ((Long) extraData.getOrDefault("z", 0L)).intValue();

        World world = Bukkit.getWorld(worldName);
        if(world == null)
            throw new IllegalArgumentException("Invalid world: " + worldName);
        this.destination = new Location(world, x, y, z);
        this.range = Math.max(0, ((Long) extraData.getOrDefault("range", 0L)).intValue());

        this.display = "§bGo to §d" + Tools.enumNameToHumanReadable(world.getName()) + ": " + x + "x, " + y + "y, " + z + "z";
        this.description = new String[]{"§eGet within §6" + range + " blocks", "§eof this location to",
                "§ecomplete this mission."};

        bossBarInformation.setTitle(display);
        bossBarInformation.setStyle(BarStyle.SOLID);
        bossBarInformation.setColor(BarColor.GREEN);
        bossBarInformation.setFlags(); // No flags
    }

    @Override
    protected void setActive(boolean active) {
        super.setActive(active);

        if(active) {
            if(task != null)
                return;

            // Mission activated, no task running, create one
            task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                // For each player, calculate their distance from destination, track that distance and
                // their maximum recorded distance. Use those two values to calculate the progress bar's
                // percentage. If distance isn't applicable because the player is in different worlds, skip.
                // If a record for a player is missing, create it. If a player finishes the mission,
                // destroy their record. Skip all players who've completed the mission or whose
                // data has not loaded yet.
                for(Player pl : Bukkit.getOnlinePlayers()) {
                    Long progress = getProgress(pl); // Get progress
                    if(progress == null || progress >= maxProgress) // Skip unloaded progress players + completed mission players.
                        continue;

                    TraversalTracker tracker = traversal.get(pl.getUniqueId());
                    if(tracker == null) { // Add to record
                        tracker = new TraversalTracker();
                        traversal.put(pl.getUniqueId(), tracker);
                    }

                    // Skip updating progress if in another world
                    if(!pl.getWorld().equals(destination.getWorld()))
                        continue;

                    // Get current distance, add to record tracker. Account for range buffer
                    double currentDistance = Math.max(pl.getLocation().distance(destination) - range, 0);
                    tracker.setCurrentDistance(currentDistance);

                    // Calculate progress bar
                    long pbar = maxProgress - (long) ((tracker.getCurrentDistance() / tracker.getMaxDistance()) * maxProgress);
                    if(pbar == progress) // If no progress, skip updating it.
                        continue;

                    setProgress(pl, pbar); // Update progress bar
                    if(pbar >= maxProgress) { // If mission completed, remove record tracker.
                        traversal.remove(pl.getUniqueId());
                    }
                }
            }, 11L, 11L); // Do once every 11 ticks for maximum TPS spread.
        } else {
            if(task == null) // No task running, don't need to kill anything
                return;

            task.cancel(); // Kill the task, stop tracking players.
            traversal.clear(); // Clear existing record data
            task = null;
        }
    }

    /**
     * Temporary data unit to keep track of the current and maximum distances seen
     * between a player and destination.
     */
    private static class TraversalTracker {

        private double currentDistance = 0, maxDistance = 0;

        public double getCurrentDistance() {
            return currentDistance;
        }

        /**
         * Update the current distance between player and destination. If larger than the maximum
         * distance seen so far, auto update the max distance.
         * @param currentDistance The player's current distance
         */
        public void setCurrentDistance(double currentDistance) {
            this.currentDistance = currentDistance;
            if(currentDistance > maxDistance)
                maxDistance = currentDistance;
        }

        public double getMaxDistance() {
            return maxDistance;
        }
    }
}
