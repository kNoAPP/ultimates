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
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;

@CardInfo(
        material = Material.LEATHER_BOOTS,
        name = "runners-diet",
        display = "§bRunner's Diet", // Typically we want the color to match the Primal
        description = {"§7When running, you don't", "§7go below §e8 §7hunger"},
        source = PrimalSource.SKY,
        tier = Tier.COMMON)
public class RunnersDietCard extends Card {

    public RunnersDietCard(Ultimates plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerRunEvent(PlayerMoveEvent playerMoveEvent) {
        Player player = playerMoveEvent.getPlayer();

        if(drawn.contains(player) && player.isSprinting()) {
            if(player.getFoodLevel() < 10) {
                player.setFoodLevel(player.getFoodLevel() + 1);
                player.setSaturation(5);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerHungerChange(FoodLevelChangeEvent foodLevelChangeEvent) {
        Player player = (Player) foodLevelChangeEvent.getEntity();

        if(drawn.contains(player) && player.isSprinting()) {
            if(foodLevelChangeEvent.getFoodLevel() < 10) {
                foodLevelChangeEvent.setCancelled(true);
            }
        }
    }
}
