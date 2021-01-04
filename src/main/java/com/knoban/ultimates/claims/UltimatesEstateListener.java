package com.knoban.ultimates.claims;

import com.knoban.atlas.claims.Estate;
import com.knoban.atlas.claims.EstatePermission;
import com.knoban.atlas.claims.LandManager;
import com.knoban.ultimates.Ultimates;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.jetbrains.annotations.NotNull;

public class UltimatesEstateListener implements Listener {

    public static final String SHOOTER = "ults_block_shooter";

    private Ultimates plugin;
    private LandManager lm;

    public UltimatesEstateListener(@NotNull Ultimates plugin, @NotNull LandManager lm) {
        this.plugin = plugin;
        this.lm = lm;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockPhase(EntityChangeBlockEvent e) {
        Entity shot = e.getEntity();
        if(e.getEntityType() == EntityType.FALLING_BLOCK) {
            Block b = e.getBlock();
            Chunk chunk = b.getChunk();

            Estate estate = lm.getEstate(chunk);
            if(estate != null) {
                if(shot.hasMetadata(SHOOTER)) {
                    Player shooter = (Player) shot.getMetadata(SHOOTER).get(0).value();
                    if(shooter == null || !estate.hasPermission(shooter.getUniqueId(), EstatePermission.PLACE.getCode())) {
                        e.setCancelled(true);
                        shot.remove();

                        if(estate.getOwner() != null) {
                            OfflinePlayer owner = Bukkit.getOfflinePlayer(estate.getOwner());
                            if(shooter.isOnline())
                                shooter.sendMessage("§cYou need §4" + owner.getName() + "'s §cpermission to do that.");
                        } else if(shooter.isOnline())
                            shooter.sendMessage("§cYou need permission to do that.");
                    }
                } else {
                    e.setCancelled(true);
                    shot.remove();
                }
            }
        }
    }
}
