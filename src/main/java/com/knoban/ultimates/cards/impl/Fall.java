package com.knoban.ultimates.cards.impl;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.cards.base.Silenceable;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;

@CardInfo(
        material = Material.FEATHER,
        name = "feather-weight",
        display = "§bFeather Weight", // Typically we want the color to match the Primal
        description = {"§7You are immune to fall damage!"},
        source = PrimalSource.SKY,
        tier = Tier.RARE
)
public class Fall extends Card implements Silenceable {

    public Fall(Ultimates plugin) {
        super(plugin);
    }

    //normal priority: we modify the damage amount
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent e) {
        if(e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            if(drawn.contains(p) && e.getCause() == EntityDamageEvent.DamageCause.FALL) {
                e.setDamage(0);
                e.setCancelled(true);
            }
        }
    }
}

