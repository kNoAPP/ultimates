package com.knoban.ultimates.cards.impl;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

@CardInfo(
        material = Material.LEAD,
        name = "worms-burrow",
        display = "§7Worm's Burrow", // Typically we want the color to match the Primal
        description = {"§7You can §ebreak §7dirt", "§7blocks §cinstantly."},
        source = PrimalSource.MOON,
        tier = Tier.RARE
)
public class WormCard extends Card {

    public WormCard(Ultimates plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDirtClick(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if(drawn.contains(p) && e.getHand() == EquipmentSlot.HAND && e.getAction() == Action.LEFT_CLICK_BLOCK
                && (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE)) {
            Block clicked = e.getClickedBlock();
            if(clicked.getType() == Material.DIRT) {
                BlockBreakEvent blockBreak = new BlockBreakEvent(clicked, p);
                Bukkit.getPluginManager().callEvent(blockBreak);
                if(!blockBreak.isCancelled()) {
                    e.setCancelled(true);
                    clicked.breakNaturally();
                }
            }
        }
    }
}
