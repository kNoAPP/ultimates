package com.knoban.ultimates.cards.impl;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import java.util.Random;

@CardInfo(
        material = Material.COOKED_BEEF,
        name = "hot-hands",
        display = "§cHot Hands", // Typically we want the color to match the Primal
        description = {"§7Punching players or mobs", "§7will §4ignite §7them for", "§e2 seconds."},
        source = PrimalSource.FIRE,
        tier = Tier.RARE
)
public class HotHandsCard extends Card {

    private static Random rand = new Random();

    public HotHandsCard(Ultimates plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if(e.getDamager() instanceof Player && e.getEntity() instanceof LivingEntity) {
            Player p = (Player) e.getDamager();
            if(drawn.contains(p) && e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK && p.getInventory().getItemInMainHand().getType() == Material.AIR) {
                LivingEntity le = (LivingEntity) e.getEntity();
                le.setFireTicks(40);
            }
        }
    }
}