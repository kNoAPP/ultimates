package com.knoban.ultimates.cards.impl;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Random;

@CardInfo(
        material = Material.EXPERIENCE_BOTTLE,
        name = "enlightened",
        display = "§6Enlightened", // Typically we want the color to match the Primal
        description = {"§7When §6the sun §7calls", "§7you follow. And those who", "§7follow are §eEnlightened§7."},
        source = PrimalSource.SUN,
        tier = Tier.EPIC
)
public class EnlightenedCard extends Card {

    private BukkitTask task;

    public EnlightenedCard(Ultimates plugin) {
        super(plugin);
    }

    @Override
    protected void register() {
        super.register();
        final Random random = new Random();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if(random.nextInt(3) == 0) {
                int amt = random.nextInt(6) + 2;
                for(Player p : drawn) {
                    long time = p.getWorld().getTime();
                    if((time < 12786 || time > 23215) && p.getLocation().getBlock().getLightLevel() >= 13
                            && p.getWorld().getHighestBlockAt(p.getLocation()).getY() - 2 < p.getLocation().getBlockY())
                        p.giveExp(amt, true);
                }
            }
        }, 50L, 50L);
    }


    @Override
    protected void unregister() {
        super.unregister();
        task.cancel();
        task = null;
    }
}
