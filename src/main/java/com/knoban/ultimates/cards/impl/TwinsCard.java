package com.knoban.ultimates.cards.impl;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Material;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityBreedEvent;

@CardInfo(
        material = Material.WHEAT,
        name = "fertility-doctor",
        display = "§9Fertility Doctor", // Typically we want the color to match the Primal
        description = {"§7Breeding animals results", "§7in §btwins§7."},
        source = PrimalSource.NONE,
        tier = Tier.COMMON
)
public class TwinsCard extends Card {

    public TwinsCard(Ultimates plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityBreedEvent e) {
        if(e.getBreeder() instanceof Player) {
            Player p = (Player) e.getBreeder();
            if(drawn.contains(p)) {
                LivingEntity father = e.getFather();
                Entity twin = father.getWorld().spawnEntity(father.getLocation(), father.getType());
                if(twin == null)
                    return;

                twin.setTicksLived(1);
                if(twin instanceof Ageable) {
                    ((Ageable) twin).setBaby();
                }
            }
        }
    }
}