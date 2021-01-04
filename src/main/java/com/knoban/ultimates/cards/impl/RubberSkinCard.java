package com.knoban.ultimates.cards.impl;

import com.destroystokyo.paper.event.entity.ProjectileCollideEvent;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

@CardInfo(
        material = Material.LEATHER_CHESTPLATE,
        name = "rubber-skin",
        display = "§aRubber Skin", // Typically we want the color to match the Primal
        description = {"§9Projectiles §7bounce right", "§7off you and are §ereflected", "§7towards the source."},
        source = PrimalSource.EARTH,
        tier = Tier.RARE
)
public class RubberSkinCard extends Card {

    public RubberSkinCard(Ultimates plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileAboutToLand(ProjectileCollideEvent e) {
        if(e.getCollidedWith() instanceof Player) {
            Player p = (Player) e.getCollidedWith();
            if(drawn.contains(p)) {
                e.setCancelled(true);

                Projectile projectile = e.getEntity();
                projectile.setVelocity(projectile.getVelocity().multiply(-1));
            }
        }
    }
}
