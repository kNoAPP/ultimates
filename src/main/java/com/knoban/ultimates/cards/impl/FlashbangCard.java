package com.knoban.ultimates.cards.impl;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@CardInfo(
        material = Material.EGG,
        name = "flashbang",
        display = "§cFlashbang Eggs", // Typically we want the color to match the Primal
        description = {"§eEggs §7you throw will", "§cexplode §7on landing stunning", "§7hit §2players and mobs§7!"},
        source = PrimalSource.FIRE,
        tier = Tier.RARE
)
public class FlashbangCard extends Card {

    public static final String METADATA = "ults_flashbang";

    public static final int EFFECT_ON_MOBS_SECS = 5;

    public FlashbangCard(Ultimates plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEggThrow(PlayerEggThrowEvent e) {
        Player p = e.getPlayer();
        if(drawn.contains(p)) {
            e.setHatching(false);
            e.getEgg().setMetadata(METADATA, new FixedMetadataValue(plugin, true));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEggLand(ProjectileHitEvent e) {
        Projectile proj = e.getEntity();
        if(proj.hasMetadata(METADATA)) {
            proj.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, proj.getLocation(), 1,
                    0F, 0F, 0F, 0.01);
            proj.getWorld().playSound(proj.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1F, 1F);
            Entity hit = e.getHitEntity();
            if(hit instanceof Mob) {
                Mob mob = (Mob) hit;
                mob.setAware(false);
                mob.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, EFFECT_ON_MOBS_SECS * 20,
                        0, false, true, true));
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if(mob.isValid())
                        mob.setAware(true);
                }, EFFECT_ON_MOBS_SECS * 20L);
            } else if(hit instanceof Player) {
                Player t = (Player) hit;
                t.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                t.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 120, 0));
            }
        }
    }
}