package com.knoban.ultimates.cards.impl;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

@CardInfo(
        material = Material.BOW,
        name = "strange-bow",
        display = "§9Strange Bow", // Typically we want the color to match the Primal
        description = {"§7Your bows shoot arrows...", "§7yes, §oarrows§7..."},
        source = PrimalSource.NONE,
        tier = Tier.COMMON
)
public class StrangeBowCard extends Card {

    public static final String METADATA = "ults_itemstack";

    public StrangeBowCard(Ultimates plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent e) {
        if(e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            if(drawn.contains(p) && e.getProjectile() instanceof Projectile) {
                Projectile proj = (Projectile) e.getProjectile();
                proj.setMetadata(METADATA, new FixedMetadataValue(plugin, e.getArrowItem()));
                passProjectile(proj);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMerge(ItemMergeEvent e) {
        Item entity = e.getEntity();
        Item target = e.getTarget();

        if(entity.hasMetadata(METADATA) || target.hasMetadata(METADATA)) {
            e.setCancelled(true);
        }
    }

    /**
     * Grant the effects of this card to a projectile. This class will handle
     * the projectile going forward.
     * MUST contain METADATA with the itemstack of the arrow.
     * @param proj - The projectile to grant these effects to.
     */
    public void passProjectile(Projectile proj) {
        if(!proj.hasMetadata(METADATA))
            throw new IllegalArgumentException(
                    "ItemStackBowCard passProjectile requires the project's ItemStack in the metadata! (" + METADATA + ")");

        // This is for compatibility reasons with other cards. Gives other ProjectileEvents a chance to run before
        // We start messing with projectile physics.
        new BukkitRunnable() {
            public void run() {
                ItemStack arrowIS = ((ItemStack) proj.getMetadata(METADATA).get(0).value()).clone();
                arrowIS.setAmount(1);

                Item newArrow = proj.getWorld().dropItem(proj.getLocation(), arrowIS);
                newArrow.setPickupDelay(10000);
                newArrow.setTicksLived(proj.getTicksLived()+1);
                newArrow.setVelocity(proj.getVelocity().clone());
                newArrow.setInvulnerable(true);
                newArrow.setFireTicks(proj.getFireTicks());
                newArrow.setGravity(proj.hasGravity());

                new BukkitRunnable() {
                    Vector lastVelocity = proj.getVelocity().clone();
                    int i = 0;
                    public void run() {
                        if(!newArrow.isValid() || newArrow.isDead()
                                || (++i > 5 && newArrow.isOnGround()) || newArrow.getNearbyEntities(2, 2, 2).stream().anyMatch(e -> e != proj.getShooter() && e instanceof LivingEntity)) {
                            if(proj.isValid() && !proj.isDead()) {
                                proj.setGravity(newArrow.hasGravity());
                                proj.teleport(newArrow.getLocation());
                                proj.setVelocity(lastVelocity);
                                proj.setFireTicks(newArrow.getFireTicks());
                            }

                            newArrow.remove();
                            this.cancel();
                        } else if(i >= 1200) {
                            newArrow.remove();
                            proj.remove();
                            this.cancel();
                        } else
                            lastVelocity = newArrow.getVelocity().clone();
                    }
                }.runTaskTimer(plugin, 0L, 1L);

                Location high = proj.getLocation().clone();
                high.setY(5000);
                proj.teleport(high);
                proj.setGravity(false);
                proj.setVelocity(new Vector());
            }
        }.runTaskLater(plugin, 1L);
    }
}
