package com.knoban.ultimates.cards.impl;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

@CardInfo(
        material = Material.LEATHER_BOOTS,
        name = "juggernaut",
        display = "§9Juggernaut", // Typically we want the color to match the Primal
        description = {"§7The faster you travel,", "§7the faster you travel."},
        source = PrimalSource.NONE,
        tier = Tier.COMMON
)
public class JuggernautCard extends Card {

    private static final long DISTANCE_PER_SPEED_AMPL = 5;
    private BukkitTask task;
    private HashMap<Player, Location> lastSeenLocation = new HashMap<>();

    public JuggernautCard(Ultimates plugin) {
        super(plugin);
    }

    @Override
    protected void register() {
        super.register();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for(Player p : lastSeenLocation.keySet()) {
                Location current = p.getLocation().clone();
                Location last = lastSeenLocation.put(p, current);

                assert last != null;
                double distance = manhattanDistance(current, last);
                int ampl = (int) (distance - 12) / 2;

                if(ampl < 0)
                    return;
                else if(ampl > 10)
                    ampl = 10;

                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, ampl, true, true));
            }
        }, 38L, 38L);
    }

    // Faster calculation than Location#distance, but a little less precise.
    private double manhattanDistance(@NotNull Location a, @NotNull Location b) {
        if(!a.getWorld().equals(b.getWorld()))
            return -1;

        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY()) + Math.abs(a.getZ() - b.getZ());
    }

    @Override
    protected void unregister() {
        super.unregister();
        task.cancel();
        task = null;
    }

    @Override
    public boolean draw(Player p) {
        boolean didEquip = super.draw(p);
        if(didEquip) {
            lastSeenLocation.put(p, p.getLocation().clone());
        }
        return didEquip;
    }

    @Override
    public boolean discard(Player p) {
        boolean didDispose = super.discard(p);
        if(didDispose) {
            lastSeenLocation.remove(p);
        }
        return didDispose;
    }
}
