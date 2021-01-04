package com.knoban.ultimates.cards.impl;

import com.knoban.atlas.utils.Tools;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

@CardInfo(
        material = Material.TALL_GRASS,
        name = "scavenger",
        display = "§aScavenger", // Typically we want the color to match the Primal
        description = {"§7Breaking §2tall grass§7 has", "§7a chance to §bdrop loot§7."},
        source = PrimalSource.EARTH, tier = Tier.RARE)
public class ScavengerCard extends Card {

    public ScavengerCard(Ultimates plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if(drawn.contains(p)) {
            Block b = e.getBlock();
            if(b.getType().equals(Material.TALL_GRASS)) {
                int range = Tools.randomNumber(1, 100);
                int randomAmount = Tools.randomNumber(1, 3);
                Location loc = b.getLocation();
                ItemStack is = getItem(range, randomAmount);
                if(is != null) {
                    b.getWorld().dropItemNaturally(loc, is);
                    if(range > 60) {
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5F, 2F);
                        p.sendMessage("§aYou scavenged " + randomAmount + " " + is.getType().name());
                    }
                }
            }
        }
    }

    private String fixName(String itemName) {
        if(itemName.length() <= 0)
            return "";

        itemName = itemName.substring(0, 1).toUpperCase() + itemName.substring(1).toLowerCase();
        itemName = itemName.replaceAll("_", " ");
        return itemName;
    }

    private ItemStack getItem(Integer range, Integer random) {
        if(range <= 60) {
            return null;
        } else if(range <= 70) {
            return new ItemStack(Material.BONE_MEAL, random); // 9% bone meal
        } else if(range <= 77) {
            return new ItemStack(Material.COOKED_PORKCHOP, random);// 7% cooked porkchop
        } else if(range <= 84) {
            return new ItemStack(Material.GLASS_BOTTLE, random); // 7% glass bottle
        } else if(range <= 94) {
            return new ItemStack(Material.GOLD_NUGGET, random); // 10% gold nugget
        } else if(range <= 97) {
            return new ItemStack(Material.IRON_INGOT, random); // 3% iron
        } else if(range <= 100) {
            return new ItemStack(Material.EMERALD, random); // 2% emerald
        } else {
            return new ItemStack(Material.DIAMOND, random);// 1% diamond
        }
    }
}
