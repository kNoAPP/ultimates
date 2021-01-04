package com.knoban.ultimates.cards.impl;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

@CardInfo(
        material = Material.COMPASS,
        name = "bounty-hunter",
        display = "§8Bounty Hunter", // Typically we want the color to match the Primal
        description = {"§7Gain a compass that", "§6tracks §7the nearest player."},
        source = PrimalSource.DARK,
        tier = Tier.RARE
)
public class BountyHunterCard extends Card {

    private HashMap<UUID, ItemStack> compasses = new HashMap<>();
    private BukkitTask task;

    public BountyHunterCard(Ultimates plugin) {
        super(plugin);
    }

    private static ItemStack getTrackingCompass(Player p) {
        ItemStack is = new ItemStack(Material.COMPASS);
        CompassMeta im = (CompassMeta) is.getItemMeta();

        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for(Player t : Bukkit.getOnlinePlayers()) {
            if(p.equals(t) || !p.getWorld().equals(t.getWorld()))
                continue;

            double distance = p.getLocation().distanceSquared(t.getLocation());
            if(distance < nearestDistance) {
                nearest = t;
                nearestDistance = distance;
            }
        }

        im.setLodestoneTracked(false);
        im.setLore(Arrays.asList(LOCKED_METADATA_LORE));

        if(nearest == null) {
            im.setDisplayName("§cNearest Player §f- [§4 NONE §f]");
            im.setLodestone(p.getLocation().clone());
        } else {
            im.setDisplayName("§cNearest Player §f- [ §4" + nearest.getName() + " §f]");
            im.setLodestone(nearest.getLocation().clone());
        }

        is.setItemMeta(im);
        return is;
    }

    @Override
    protected void register() {
        super.register();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for(Player p : drawn) {
                ItemStack compass = compasses.remove(p.getUniqueId());
                // int slot = p.getInventory().first(compass);

                // Temporary fix until paper fixes the lodestone glitch
                int slot = p.getInventory().first(Material.COMPASS);
                p.getInventory().remove(Material.COMPASS);

                ItemStack newCompass = getTrackingCompass(p);
                compasses.put(p.getUniqueId(), newCompass);

                if(slot < 0)
                    p.getInventory().addItem(newCompass);
                else
                    p.getInventory().setItem(slot, newCompass);
            }
        }, 120L, 120L);
    }

    @Override
    protected void unregister() {
        super.unregister();
        task.cancel();
        task = null;
    }

    @Override
    public boolean draw(Player p) {
        boolean toRet = super.draw(p);
        if(toRet) {
            ItemStack compass = getTrackingCompass(p);
            compasses.put(p.getUniqueId(), compass);
            p.getInventory().addItem(compass);
        }
        return toRet;
    }

    @Override
    public boolean discard(Player p) {
        boolean toRet = super.discard(p);
        if(toRet) {
            ItemStack compass = compasses.remove(p.getUniqueId());
            p.getInventory().removeItemAnySlot(compass);
        }
        return toRet;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        if(drawn.contains(p)) {
            ItemStack compass = compasses.get(p.getUniqueId());
            if(!p.getInventory().contains(compass)) {
                p.getInventory().addItem(compass);
            }
        }
    }
}