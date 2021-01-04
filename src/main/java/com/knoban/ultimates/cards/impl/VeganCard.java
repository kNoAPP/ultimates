package com.knoban.ultimates.cards.impl;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.UUID;

@CardInfo(
        material = Material.GRASS_BLOCK,
        name = "veganism",
        display = "§aVeganism", // Typically we want the color to match the Primal
        description = {"§eRight-click §7grass blocks with your", "§7hand to restore hunger.", "", "§cYou cannot eat meat!"},
        source = PrimalSource.EARTH,
        tier = Tier.RARE
)
public class VeganCard extends Card {

    private HashSet<UUID> alertedRecently = new HashSet<UUID>();

    private static final EnumSet<Material> DISALLOWED_FOODS = EnumSet.of(
            Material.TROPICAL_FISH, Material.PUFFERFISH, Material.COD, Material.SALMON, Material.CHICKEN, Material.RABBIT,
            Material.MUTTON, Material.PORKCHOP, Material.BEEF, Material.SPIDER_EYE, Material.ROTTEN_FLESH, Material.COOKED_RABBIT,
            Material.COOKED_CHICKEN, Material.COOKED_COD, Material.COOKED_SALMON, Material.COOKED_MUTTON, Material.COOKED_PORKCHOP,
            Material.COOKED_BEEF, Material.RABBIT_STEW
    );

    public VeganCard(Ultimates plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDirtClick(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if(drawn.contains(p) && e.getHand() == EquipmentSlot.HAND
                && e.getAction() == Action.RIGHT_CLICK_BLOCK && (e.getItem() == null || e.getItem().getType() == Material.AIR)) {
            Block clicked = e.getClickedBlock();
            if(clicked.getType() == Material.GRASS_BLOCK && p.getFoodLevel() < 20) {
                BlockBreakEvent blockBreak = new BlockBreakEvent(clicked, p);
                Bukkit.getPluginManager().callEvent(blockBreak);
                if(!blockBreak.isCancelled()) {
                    e.setCancelled(true);
                    clicked.setType(Material.DIRT);
                    p.setFoodLevel(p.getFoodLevel() + 1);
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_EAT, 1F, 1F);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent e) {
        Player p = e.getPlayer();
        if(drawn.contains(p)) {
            ItemStack toConsume = e.getItem();
            if(toConsume != null && DISALLOWED_FOODS.contains(toConsume.getType())) {
                e.setCancelled(true);
                if(!alertedRecently.contains(p.getUniqueId())) {
                    p.sendMessage("§cYou cannot eat meat with the Veganism card drawn!");
                    p.playSound(p.getLocation(), Sound.ENTITY_FOX_SPIT, 1F, 0.8F);
                    alertedRecently.add(p.getUniqueId());
                    plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin,
                            () -> alertedRecently.remove(p.getUniqueId()), 100L);
                }
            }
        }
    }
}
