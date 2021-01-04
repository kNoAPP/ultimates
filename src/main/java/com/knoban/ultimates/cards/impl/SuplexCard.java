package com.knoban.ultimates.cards.impl;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.events.PlayerSuplexEvent;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.spigotmc.event.entity.EntityDismountEvent;

@CardInfo(
        material = Material.RABBIT_FOOT,
        name = "tavern-brawler",
        display = "§8Tavern Brawler", // Typically we want the color to match the Primal
        description = {"§eShift + Right-click §7a §6Player", "§7or §6Mob §7to pick them up.", "", "§eLeft-click §7to throw them."},
        source = PrimalSource.DARK,
        tier = Tier.EPIC
)
public class SuplexCard extends Card {

    public static final String METADATA = "ults_suplex_slime";

    public SuplexCard(Ultimates plugin) {
        super(plugin);
    }

    /**
     * IMPORTANT ASSUMPTIONS MADE IN THIS CLASS
     * 1. The Suplex Slime mount always has METADATA defined above
     * 2. The Suplex Slime mount is always mounted to a Suplexer (Player) or nothing and in air
     * 3. The Suplex Slime mount will always have a passenger
     * 4. When the Suplexee dies/disconnects/dismounts (not in air), the Slime will be removed
     * 5. When the Suplex Slime is thrown, travels through the air, and hits the ground, it despawns
     * 6. When the Suplexer disconnects/discards the Card, Suplex Slime despawns
     */

    @Override
    public boolean discard(Player p) {
        boolean didDispose = super.discard(p);
        if(didDispose) {
            if(p.getPassengers().size() > 0) {
                Entity entity = p.getPassengers().get(0);
                if(entity.hasMetadata(METADATA))
                    entity.remove();
            }
        }
        return didDispose;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickupEntity(PlayerInteractAtEntityEvent e) {
        Player p = e.getPlayer();
        if(drawn.contains(p) && p.isSneaking() && p.getGameMode() != GameMode.SPECTATOR) {
            if(p.getPassengers().size() == 0) {
                e.setCancelled(true);
                Entity entity = e.getRightClicked();
                if(entity.getVehicle() == null) {
                    PlayerSuplexEvent pse = new PlayerSuplexEvent(p, e.getRightClicked());
                    Bukkit.getServer().getPluginManager().callEvent(pse);
                    if(!pse.isCancelled()) {
                        Slime s = (Slime) p.getWorld().spawnEntity(p.getLocation(), EntityType.SLIME);
                        s.setSize(3);
                        s.setSilent(true);
                        s.setAI(false);
                        s.setCollidable(false);
                        s.setWander(false);
                        s.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 1000000, 0, true, false, false));
                        s.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 1000000, 100, true, false, false));
                        s.setMetadata(METADATA, new FixedMetadataValue(plugin, true));
                        s.addPassenger(pse.getSuplexee());
                        p.addPassenger(s);
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 1000000, 1, true));
                    }
                }
            }
        }
    }

    public static final double SUPLEX_POWER = 1.5;
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onThrowEntity(EntityDamageByEntityEvent e) {
        Entity entity = e.getEntity();
        if(entity.hasMetadata(METADATA) && e.getDamager() == entity.getVehicle()) {
            e.setCancelled(true);
            Player p = (Player) e.getEntity().getVehicle();
            Vector launchVelocity = p.getLocation().getDirection().clone().normalize().multiply(SUPLEX_POWER);
            Slime s = (Slime) p.getPassengers().get(0);
            p.removePassenger(s);
            s.setAI(true);
            s.setVelocity(launchVelocity);
            new BukkitRunnable() {
                public void run() {
                    if(!s.isValid() || s.isOnGround()) {
                        s.remove();
                        this.cancel();
                    }
                }
            }.runTaskTimer(plugin, 2L, 2L);
            p.removePotionEffect(PotionEffectType.SLOW);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAttemptDismount(EntityDismountEvent e) {
        Entity entity = e.getEntity();
        Entity mount = e.getDismounted();
        if(mount.isValid() && mount.hasMetadata(METADATA)) {
            Player p = (Player) e.getDismounted().getVehicle();
            if(p == null) {
                e.setCancelled(true);
                return;
            }

            p.removePotionEffect(PotionEffectType.SLOW);
            mount.remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent e) {
        Entity entity = e.getEntity();
        Entity mount = entity.getVehicle();
        if(mount != null && mount.hasMetadata(METADATA)) {
            mount.remove();
        }

        if(entity instanceof Player) {
            Player p = (Player) entity;
            if(drawn.contains(p)) {
                p.eject();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        Entity mount = player.getVehicle();
        if(mount != null && mount.hasMetadata(METADATA)) {
            mount.remove();
        }
    }
}
