package com.knoban.ultimates.cards.impl;

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

@CardInfo(
        material = Material.ANVIL,
        name = "anchor",
        display = "§3Anchor", // Typically we want the color to match the Primal
        description = {"§7You deal no knockback", "§7and take no knockback."},
        source = PrimalSource.OCEAN,
        tier = Tier.RARE
)
public class AnchorCard extends Card {

    public AnchorCard(Ultimates plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onKnockback(EntityKnockbackByEntityEvent e) {
        LivingEntity knockedBack = e.getEntity();
        Entity hitBy = e.getHitBy();

        if(drawn.contains(knockedBack) || drawn.contains(hitBy))
            e.setCancelled(true);
    }
}