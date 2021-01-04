package com.knoban.ultimates.cards.impl;

import com.destroystokyo.paper.event.entity.ProjectileCollideEvent;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.cards.Cards;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.EnumSet;

@CardInfo(
        material = Material.SPECTRAL_ARROW,
        name = "rubber-arrows",
        display = "§bRubber Arrows", // Typically we want the color to match the Primal
        description = {"§7Your arrows §ebounce §7up to", "§7three times before landing."},
        source = PrimalSource.SKY,
        tier = Tier.RARE
)
public class RubberProjectileCard extends Card {

    public static final String METADATA_BOUNCE = "ults_rubber_projectile_bounce";

    public static final int MAX_BOUNCES = 2;
    public static final double POWER_AFTER_BOUNCE = 0.8; // Where 0 = 0%, 1 = 100% of previous initial velocity
    public static final EnumSet<EntityType> BOUNCING_PROJECTILE_TYPES = EnumSet.of(
            EntityType.ARROW, EntityType.SPECTRAL_ARROW
    );

    public RubberProjectileCard(Ultimates plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLaunchArrow(ProjectileLaunchEvent e) {
        Projectile proj = e.getEntity();
        ProjectileSource source = proj.getShooter();
        if(source instanceof Player) {
            Player shooter = (Player) source;
            if(drawn.contains(shooter) && BOUNCING_PROJECTILE_TYPES.contains(proj.getType()) && !proj.hasMetadata(METADATA_BOUNCE)) {
                proj.setMetadata(METADATA_BOUNCE, new FixedMetadataValue(plugin, MAX_BOUNCES));
                proj.setMetadata(METADATA_MAGNITUDE, new FixedMetadataValue(plugin, proj.getVelocity().length()));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjCollide(ProjectileCollideEvent e) {
        Projectile proj = e.getEntity();
        if(proj.hasMetadata(METADATA_BOUNCE)) {
            proj.setMetadata(METADATA_BOUNCE, new FixedMetadataValue(plugin, 0));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjHit(ProjectileHitEvent e) {
        Projectile proj = e.getEntity();

        if(proj.hasMetadata(METADATA_BOUNCE)) {
            int bounces = (Integer) proj.getMetadata(METADATA_BOUNCE).get(0).value();

            if(bounces == 0)
                return;

            Vector vel = proj.getVelocity();
            Location loc = proj.getLocation();
            Block hitBlock = loc.getBlock();
            BlockFace blockFace = null;
            BlockIterator blockIterator = new BlockIterator(loc.getWorld(), loc.toVector(), vel, 0.0D, 3);
            Block previousBlock = hitBlock;
            Block nextBlock = blockIterator.next();
            while(blockIterator.hasNext() && (nextBlock.getType() == Material.AIR || nextBlock.isLiquid() || nextBlock.equals(hitBlock))) {
                previousBlock = nextBlock;
                nextBlock = blockIterator.next();
            }
            blockFace = nextBlock.getFace(previousBlock);
            if(blockFace != null) {
                if(blockFace == BlockFace.SELF) {
                    blockFace = BlockFace.UP;
                }
                Vector mirrorDirection = new Vector(blockFace.getModX(), blockFace.getModY(), blockFace.getModZ());
                double dotProduct = vel.dot(mirrorDirection);
                mirrorDirection = mirrorDirection.multiply(dotProduct).multiply(2.0D);
                Projectile newProjectile = (Projectile) proj.getWorld().spawnEntity(loc, proj.getType());
                double mag = (double) proj.getMetadata(METADATA_MAGNITUDE).get(0).value() * POWER_AFTER_BOUNCE;
                newProjectile.setVelocity(vel.subtract(mirrorDirection).normalize().multiply(mag)); //Changeable
                newProjectile.setShooter(proj.getShooter());
                newProjectile.setFireTicks(proj.getFireTicks());
                newProjectile.setTicksLived(proj.getTicksLived()+1);
                newProjectile.setGravity(proj.hasGravity());
                if(proj instanceof Arrow) {
                    Arrow arrow = (Arrow) proj;
                    Arrow newArrow = (Arrow) newProjectile;
                    if(arrow.hasCustomEffects())
                        newArrow.setBasePotionData(arrow.getBasePotionData());
                    newArrow.setPickupStatus(arrow.getPickupStatus());
                }
                newProjectile.setMetadata(METADATA_BOUNCE, new FixedMetadataValue(Ultimates.getPlugin(), bounces-1));
                newProjectile.setMetadata(METADATA_MAGNITUDE, new FixedMetadataValue(plugin, mag));

                // Special Case for ItemStackBowCard
                if(proj.hasMetadata(StrangeBowCard.METADATA)) {
                    newProjectile.setMetadata(StrangeBowCard.METADATA, proj.getMetadata(StrangeBowCard.METADATA).get(0));
                    Cards.getInstance().getCardInstance(StrangeBowCard.class).passProjectile(newProjectile);
                }

                // Special Case for ZeroGravityProjectileCard
                if(proj.hasMetadata(ZeroGravityProjectileCard.METADATA)) {
                    newProjectile.setMetadata(ZeroGravityProjectileCard.METADATA, proj.getMetadata(ZeroGravityProjectileCard.METADATA).get(0));
                    Cards.getInstance().getCardInstance(ZeroGravityProjectileCard.class).prepareToRemove(newProjectile);
                }

                proj.remove();
            }
        }
    }
}
