package com.knoban.ultimates.cards.impl;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@CardInfo(
        material = Material.ENDER_EYE,
        name = "zero-gravity-arrows",
        display = "§bZero-Gravity Arrows", // Typically we want the color to match the Primal
        description = {"§7Your arrows are §8Zero Gravity.", "§7They travel at a §c1/2 speed", "§7but in a §2straight line."},
        source = PrimalSource.SKY,
        tier = Tier.RARE
)
public class ZeroGravityProjectileCard extends Card {

    public static final double VELOCITY_REDUCTION = 0.5;
    public static final String METADATA = "ults_zerogravity";

    public static final EnumSet<EntityType> ZG_PROJECTILE_TYPES = EnumSet.of(
            EntityType.ARROW, EntityType.SPECTRAL_ARROW
    );

    private List<Projectile> Projectile = new ArrayList<Projectile>();

    public ZeroGravityProjectileCard(Ultimates plugin) {
        super(plugin);
    }

    @Override
    public boolean discard(Player p) {
        boolean didDispose = super.discard(p);

        // This cleans up any floating projectiles when the server goes offline.
        if(didDispose && drawn.size() == 0) {
            Projectile.stream().forEach((proj) -> proj.remove());
            Projectile.clear();
        }
        return didDispose;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLaunchArrow(ProjectileLaunchEvent e) {
        Projectile proj = e.getEntity();
        ProjectileSource source = proj.getShooter();
        if(source instanceof Player) {
            Player shooter = (Player) source;
            if(drawn.contains(shooter) && ZG_PROJECTILE_TYPES.contains(proj.getType())) {
                proj.setTicksLived(1100);
                proj.setVelocity(proj.getVelocity().multiply(VELOCITY_REDUCTION));
                proj.setGravity(false);
                proj.setMetadata(METADATA, new FixedMetadataValue(plugin, true));
                proj.setMetadata(METADATA_MAGNITUDE, new FixedMetadataValue(plugin, proj.getVelocity().length()));

                prepareToRemove(proj);
            }
        }
    }

    public void prepareToRemove(Projectile proj) {
        // Remove the projectile from the air after 21s (since Paper doesn't do that for us).
        Projectile.add(proj);
        new BukkitRunnable() {
            int i = 0;
            public void run() {
                if(++i >= 7 || !proj.isValid()) {
                    Projectile.remove(proj);
                    proj.remove();
                }

                // Maintain velocity
                if(proj.getVelocity().length() > 0) {
                    double mag = (double) proj.getMetadata(METADATA_MAGNITUDE).get(0).value();
                    proj.setVelocity(proj.getVelocity().clone().normalize().multiply(mag));
                }
            }
        }.runTaskTimer(plugin, 60L, 60L);
    }
}
