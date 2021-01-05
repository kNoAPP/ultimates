package com.knoban.ultimates.cards.impl;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cardholder.CardHolder;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.ArrayList;
import java.util.List;

@CardInfo(
        material = Material.TROPICAL_FISH,
        name = "schooling",
        display = "§3Schooling", // Typically we want the color to match the Primal
        description = {"§7The more allies nearby", "§3with schooling§7, the", "§cmore damage §7you deal"},
        source = PrimalSource.OCEAN,
        tier = Tier.COMMON
)
public class SchoolingCard extends Card {

    //The range for the schooling check
    private static final double SCHOOLING_RANGE = 4.0;

    //The most amount of matches schooling cares about
    private static final int MAX_MATCHES = 3;

    //The amount of damage that should be added, multiplied by matches
    private static final double DAMAGE_MULTIPLER = 1.5;

    public SchoolingCard(Ultimates plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {

        if(event.getDamager() instanceof Player && event.getEntity() instanceof LivingEntity) {
            Player damager = (Player) event.getDamager();
            LivingEntity livingEntity = (LivingEntity) event.getEntity();

            CardHolder cardHolder = CardHolder.getCardHolder(damager);
            PrimalSource primalSource = cardHolder.getPrimalSource();
            int match = 0;

            //Find the matching primals
            for(Player player : getNearyByPlayers(damager, true)) {

                if(match >= MAX_MATCHES) {
                    break;
                }
                CardHolder targ = CardHolder.getCardHolder(player);
                PrimalSource targPrimal = targ.getPrimalSource();

                if(primalSource == targPrimal) {
                    match++;
                }
            }

            //Make sure we don't multiply by 0
            if(match > 0) {
                event.setDamage(event.getDamage() * (match * DAMAGE_MULTIPLER));
            }
        }
    }

    /**
     * Gets the nearby players surrounding the player provided
     *
     * @param player   The player o serve as the center for the search
     * @param friendly Whether or not the search should be for friendly or enemy
     * @return The {@link List} containin all nearby players fitting the criteria
     */
    private List<Player> getNearyByPlayers(Player player, boolean friendly) {
        CardHolder playerHolder = CardHolder.getCardHolder(player);
        PrimalSource playerPrimal = playerHolder.getPrimalSource();
        List<Player> returnPlayers = new ArrayList<>();

        //Search for neaby players
        for(Entity entity : player.getNearbyEntities(SCHOOLING_RANGE, SCHOOLING_RANGE / 2, SCHOOLING_RANGE)) {

            //Exclude self from the list
            if(entity.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }

            if(entity instanceof Player) {

                Player targ = (Player) entity;
                CardHolder cardHolder = CardHolder.getCardHolder(targ);
                PrimalSource primalSource = cardHolder.getPrimalSource();

                //If primals match, check if friendly
                if(playerPrimal == primalSource) {
                    if(friendly) {
                        returnPlayers.add(targ);
                    }
                }

                //Primals don't match
                else {
                    //Validate that they aren't friendly
                    if(!friendly) {
                        returnPlayers.add(targ);
                    }
                }
            }
        }
        return returnPlayers;
    }
}
