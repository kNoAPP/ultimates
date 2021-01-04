package com.knoban.ultimates.cards.impl;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

@CardInfo(
        material = Material.MAGMA_BLOCK,
        name = "magma-walker",
        display = "§cMagma Walker", // Typically we want the color to match the Primal
        description = {"§eShifting §7over §4lava §7cools", "§7it into a §cmagma block."},
        source = PrimalSource.FIRE,
        tier = Tier.RARE
)
public class MagmaWalkerCard extends Card {

    /*
     * This card was inspired by PandayTSU.
     */

    public MagmaWalkerCard(Ultimates plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMovement(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if(p.isSneaking() && drawn.contains(p) && p.getGameMode() != GameMode.SPECTATOR) { // Ensures 99.99% of the time, this event finishes super quickly.
            Block below = p.getWorld().getBlockAt(p.getLocation().clone().subtract(0, 1, 0));
            if(below.getType() == Material.LAVA) {
                BlockPlaceEvent bpe = new BlockPlaceEvent(below, below.getState(),
                        below.getWorld().getBlockAt(below.getLocation().clone().subtract(0, 1, 0)),
                                new ItemStack(Material.MAGMA_BLOCK), p, true, EquipmentSlot.HAND);
                plugin.getServer().getPluginManager().callEvent(bpe);
                if(!bpe.isCancelled()) {
                    below.setType(Material.MAGMA_BLOCK);
                } else
                    p.sendMessage("§cYou cannot use your " + info.display() + "§c card here!");
            }
        }
    }
}
