package com.knoban.ultimates.cards.impl;

import com.knoban.atlas.scheduler.ClockedTask;
import com.knoban.atlas.scheduler.ClockedTaskManager;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.UUID;

@CardInfo(
        material = Material.GLASS_BOTTLE,
        name = "watchdog",
        display = "§cWatchdog", // Typically we want the color to match the Primal
        description = {"§dStanding still §7for §e8", "§eseconds §7causes you to", "§7become completely §8invisible§7."},
        source = PrimalSource.FIRE,
        tier = Tier.RARE
)
public class TeemoCard extends Card {

    public static final int STILL_SECONDS = 8;

    private HashMap<UUID, ClockedTask> notMovingPlayers = new HashMap<>();
    private HashMap<UUID, ItemStack[]> savedArmor = new HashMap<>();

    public TeemoCard(Ultimates plugin) {
        super(plugin);
    }

    @Override
    public boolean draw(Player p) {
        boolean didEquip = super.draw(p);
        if(didEquip) {
            addMovementTask(p);
        }
        return didEquip;
    }

    @Override
    public boolean discard(Player p) {
        boolean didDispose = super.discard(p);
        if(didDispose) {
            ClockedTask task = notMovingPlayers.remove(p.getUniqueId());
            if(task != null)
                task.setCancelled(true);
            else
                show(p);
        }
        return didDispose;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if(drawn.contains(p) && e.getFrom().getWorld().equals(e.getTo().getWorld())
                && e.getFrom().distanceSquared(e.getTo()) > 0.05) {
            ClockedTask task = notMovingPlayers.remove(p.getUniqueId());
            if(task == null) {
                show(p);
            } else
                task.setCancelled(true);

            addMovementTask(p);
        }
    }

    private void addMovementTask(Player p) {
        ClockedTask task = new ClockedTask(System.currentTimeMillis() + STILL_SECONDS * 1000L, () -> {
            notMovingPlayers.remove(p.getUniqueId());
            if(drawn.contains(p)) {
                hide(p);
            }
        });
        ClockedTaskManager.getManager().addTask(task);

        notMovingPlayers.put(p.getUniqueId(), task);
    }

    private void show(Player p) {
        p.removePotionEffect(PotionEffectType.INVISIBILITY);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_CONDUIT_ACTIVATE, 1F, 0.9F);
        p.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, p.getLocation().clone().add(0, 0.5, 0),
                3, 0.3F, 0.3F, 0.3F, 0.01);

        ItemStack[] armor = savedArmor.remove(p.getUniqueId());
        p.getInventory().setHelmet(armor[0]);
        p.getInventory().setChestplate(armor[1]);
        p.getInventory().setLeggings(armor[2]);
        p.getInventory().setBoots(armor[3]);
    }

    private void hide(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100000, 0,
                true, false, true));
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_CONDUIT_DEACTIVATE, 1F, 0.5F);
        p.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, p.getLocation().clone().add(0, 0.5, 0),
                3, 0.3F, 0.3F, 0.3F, 0.01);

        ItemStack[] armor = new ItemStack[4];
        armor[0] = p.getInventory().getHelmet();
        armor[1] = p.getInventory().getChestplate();
        armor[2] = p.getInventory().getLeggings();
        armor[3] = p.getInventory().getBoots();

        p.getInventory().setHelmet(new ItemStack(Material.AIR));
        p.getInventory().setChestplate(new ItemStack(Material.AIR));
        p.getInventory().setLeggings(new ItemStack(Material.AIR));
        p.getInventory().setBoots(new ItemStack(Material.AIR));

        savedArmor.put(p.getUniqueId(), armor);
    }
}