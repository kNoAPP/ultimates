package com.knoban.ultimates.cards.impl;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.knoban.atlas.callbacks.Callback;
import com.knoban.atlas.listeners.HeldSlotListener;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

@CardInfo(
        material = Material.IRON_HOE,
        name = "reaper",
        display = "§7Reaper", // Typically we want the color to match the Primal
        description = {"§7Killing a mob while holding", "§7your §4scythe§7 will §2harvest", "§7a §2soul§6 permanently",
                "§2increasing §7the weapon's", "§7damage until the user dies."},
        source = PrimalSource.DARK, tier = Tier.LEGENDARY)

public class SoulCard extends Card {

    private HashMap<UUID, Integer> souls = new HashMap<>();

    public SoulCard(Ultimates plugin) {
        super(plugin);
    }

    @Override
    public void onDefaultPlayerData(Player p) {
        writeData(p.getUniqueId(), "souls", 0L, null);
    }

    @Override
    public void onPlayerData(Player p, Map<String, Object> data) {
        souls.put(p.getUniqueId(), ((Long) data.getOrDefault("souls", 0L)).intValue());
    }

    public static final ItemStack SCYTHE_ITEM = ScytheItem();
    private static ItemStack ScytheItem() {
        ItemStack is = new ItemStack(Material.IRON_HOE, 1);
        ItemMeta im = is.getItemMeta();
        im.setDisplayName(ChatColor.DARK_GRAY + "Scythe");
        List<String> lores = new ArrayList<>();
        lores.add(LOCKED_METADATA_LORE);
        im.setLore(lores);
        im.setUnbreakable(true);
        is.setItemMeta(im);
        return is;
    }

    @Override
    public boolean draw(Player p) {
        boolean toRet = super.draw(p);
        if(toRet) {
            p.getInventory().addItem(SCYTHE_ITEM);

            Callback callback = () -> {
                int soulCount = souls.getOrDefault(p.getUniqueId(), 0);
                p.sendActionBar("§4§oSouls §7(§c" + soulCount + "§7)");
            };

            HeldSlotListener.getInstance().setCallbacks(p, SCYTHE_ITEM, callback, callback, null);
        }
        return toRet;
    }

    @Override
    public boolean discard(Player p) {
        boolean toRet = super.discard(p);
        if(toRet) {
            p.getInventory().removeItemAnySlot(SCYTHE_ITEM);
            writeData(p.getUniqueId(), "souls", souls.remove(p.getUniqueId()), null);

            HeldSlotListener.getInstance().removeCallbacks(p, SCYTHE_ITEM);
        }
        return toRet;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobKill(EntityDeathEvent e) {
        LivingEntity victim = e.getEntity();
        Player p = victim.getKiller();
        if(p != null && drawn.contains(p)) {
            ItemStack is = p.getInventory().getItemInMainHand();
            if(is != null && is.isSimilar(SCYTHE_ITEM)) {
                victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1F, 0.7F);

                int soulCount = souls.getOrDefault(p.getUniqueId(), 0) + 1;
                souls.put(p.getUniqueId(), soulCount);
                p.sendMessage("§4You have harvested a soul...");
                if(soulCount % 20 == 0)
                    p.sendMessage("§7§oYour scythe has been upgraded!");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRespawn(PlayerPostRespawnEvent e) {
        Player p = e.getPlayer();
        if(drawn.contains(p) && !p.getInventory().contains(SCYTHE_ITEM)) {
            p.getInventory().addItem(SCYTHE_ITEM);
            p.sendMessage("§cAll of your souls were set free...");
            souls.put(p.getUniqueId(), 0);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageEvent(EntityDamageByEntityEvent e) {
        if(e.getDamager() instanceof Player) {
            Player p = (Player) e.getDamager();
            if(drawn.contains(p) && p.getInventory().getItemInMainHand().isSimilar(SCYTHE_ITEM)) {
                Entity victim = e.getEntity();
                int soulCount = souls.getOrDefault(p.getUniqueId(), 0);
                int soulLevel = soulCount / 20;
                e.setDamage(soulLevel + 2);
                victim.getWorld().spawnParticle(Particle.FALLING_DUST, victim.getLocation().clone().add(0, 1, 0),
                        2, 0.2F, 0.2F, 0.2F, Material.SAND.createBlockData());
                victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_VEX_AMBIENT, 1F, 1.7F);
            }
        }
    }
}
