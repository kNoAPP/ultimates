package com.knoban.ultimates.cards.impl;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumSet;
import java.util.HashMap;

@CardInfo(
        material = Material.POTION,
        name = "druid-heart",
        display = "§9Druid Heart", // Typically we want the color to match the Primal
        description = {"§7Gain §dRegeneration I §7whenever", "§7you are out of combat", "§7for at least §e5 seconds§7."},
        source = PrimalSource.NONE,
        tier = Tier.COMMON
)
public class OOCRegenerationCard extends Card {

    private static final long COOLDOWN_TICKS = 100L;
    private static final EnumSet<DamageCause> COMBAT =
            EnumSet.of(DamageCause.CUSTOM, DamageCause.BLOCK_EXPLOSION, DamageCause.CONTACT,
                    DamageCause.DRAGON_BREATH, DamageCause.ENTITY_ATTACK, DamageCause.ENTITY_EXPLOSION,
                    DamageCause.ENTITY_SWEEP_ATTACK, DamageCause.FIRE, DamageCause.FIRE_TICK, DamageCause.LIGHTNING,
                    DamageCause.MAGIC, DamageCause.POISON, DamageCause.PROJECTILE, DamageCause.THORNS, DamageCause.WITHER);
    private HashMap<Player, BukkitTask> ooc = new HashMap<Player, BukkitTask>();

    public OOCRegenerationCard(Ultimates plugin) {
        super(plugin);
    }

    @Override
    public boolean draw(Player p) {
        boolean didEquip = super.draw(p);
        if(didEquip) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 1000000, 0, true));
        }
        return didEquip;
    }

    @Override
    public boolean discard(Player p) {
        boolean didDispose = super.discard(p);
        if(didDispose) {
            p.removePotionEffect(PotionEffectType.REGENERATION);
            ooc.remove(p);
        }
        return didDispose;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if(e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            if(drawn.contains(p) && COMBAT.contains(e.getCause())) {
                BukkitTask task = ooc.get(p);
                if(task != null)
                    task.cancel();
                else
                    p.removePotionEffect(PotionEffectType.REGENERATION);

                ooc.put(p, plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if(p.isOnline()) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 1000000, 0, true));
                        ooc.remove(p);
                    }
                }, COOLDOWN_TICKS));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRespawn(PlayerPostRespawnEvent e) {
        Player p = e.getPlayer();
        if(drawn.contains(p)) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 1000000, 0, true));
        }
    }
}
