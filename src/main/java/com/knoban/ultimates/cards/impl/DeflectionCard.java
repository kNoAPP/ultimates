package com.knoban.ultimates.cards.impl;

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

@CardInfo(
        material = Material.CHAINMAIL_CHESTPLATE,
        name = "thick-skin",
        display = "§7Thick Skin", // Typically we want the color to match the Primal
        description = {"§7You take §c1 less damage", "§7from all sources."},
        source = PrimalSource.NONE,
        tier = Tier.COMMON
)
public class DeflectionCard extends Card {

    public DeflectionCard(Ultimates plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if(e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            if(drawn.contains(p)) {
                if(e.getDamage() > 0)
                    e.setDamage(e.getDamage() - 1);
            }
        }
    }
}
