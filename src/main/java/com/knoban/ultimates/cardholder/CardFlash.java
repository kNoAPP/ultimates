package com.knoban.ultimates.cardholder;

import com.knoban.atlas.callbacks.Callback;
import com.knoban.ultimates.cards.Card;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class CardFlash {

    private final JavaPlugin plugin;
    private final Player holder;
    private final Callback onInvalidation;
    private boolean valid;
    private BukkitTask task;

    private final ArrayList<Item> flashedCards = new ArrayList<>();
    private int a, b, c;
    private final Location center;

    private static final float RADIUS = 2.5f;
    private static final float ROTATIONS = 1f;

    public CardFlash(JavaPlugin plugin, CardHolder holder, Callback onInvalidation) {
        this.plugin = plugin;
        this.holder = holder.getPlayer();
        this.onInvalidation = onInvalidation;
        this.valid = true;
        this.a = this.b = this.c = 0;
        Player player = holder.getPlayer();
        this.center = player.getLocation().clone().add(0, 0.9, 0);
        for(Card card : holder.getDrawnCards()) {
            ItemStack is = card.getOwnedIcon();
            Item item = player.getWorld().dropItem(center, is);
            item.setGravity(false);
            item.setCanMobPickup(false);
            item.setInvulnerable(true);
            item.setPickupDelay(500000); //May be increased if needed
            //ItemMeta won't be null here unless someone forgot to define a ItemMeta for the DiscardedIcon
            item.setCustomName(is.getItemMeta().getDisplayName() + " §f- " + card.getInfo().tier().getDisplay());
            item.setCustomNameVisible(true);
            flashedCards.add(item);
        }

        this.task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::spawn, 0L, 3L);
    }

    private void spawn() {
        switch(a) {
            case 0:
                for(int j = 0; j < flashedCards.size(); j++) {
                    Item card = flashedCards.get(j);
                    Location from = card.getLocation().clone();
                    Location to = center.clone()
                            .add(RADIUS * Math.cos(2.0*Math.PI*((double)j/(double)flashedCards.size())),
                            0,
                            RADIUS * Math.sin(2.0*Math.PI*((double)j/(double)flashedCards.size())));
                    Vector velocity = to.toVector().subtract(from.toVector()).normalize().multiply(0.75);
                    card.setVelocity(velocity);
                }
                for(float i=0; i<Math.PI*2; i+=Math.PI/4)
                    center.getWorld().spawnParticle(Particle.CLOUD, center, 0, Math.cos(i), 0, Math.sin(i), 0.5);
                center.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.75F, 1.3F);
                ++a;
                break;
            case 1:
                for(Item card : flashedCards) {
                    card.setVelocity(new Vector(0, 0, 0));
                }
                task.cancel();
                task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::rotate, 10L, 12L);
        }
    }

    private void rotate() {
        if(b == 0)
            center.getWorld().playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 0.4F, 0.6F);
        if(++b < 16*ROTATIONS && holder.getWorld().equals(center.getWorld()) && holder.getLocation().distance(center) <= RADIUS) {
            if(b % 8 == 1) {
                center.getWorld().playSound(center, Sound.AMBIENT_UNDERWATER_LOOP_ADDITIONS_RARE, 1.3F, 2F);
                center.getWorld().playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.7F, 0.7F);
            }
            for(int j=0; j<flashedCards.size(); j++) {
                Item card = flashedCards.get(j);
                Location from = card.getLocation().clone();
                Location to = center.clone()
                        .add(RADIUS*Math.cos(2.0*Math.PI*((double)j/(double)flashedCards.size()) + (Math.PI/8.0)*b),
                                0,
                                RADIUS*Math.sin(2.0*Math.PI*((double)j/(double)flashedCards.size()) + (Math.PI/8.0)*b));
                Vector velocity = to.toVector().subtract(from.toVector()).normalize().multiply(0.075);
                card.setVelocity(velocity);
            }
        } else {
            task.cancel();
            this.task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::despawn, 0L, 1L);
        }
    }

    private void despawn() {
        switch(c) {
            case 0:
                for(Item card : flashedCards) {
                    Location from = card.getLocation().clone();
                    Location to = holder.getLocation();
                    Vector velocity = to.toVector().subtract(from.toVector()).multiply(0.75);
                    card.setVelocity(velocity);
                }
                center.getWorld().playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.5F, 1.8F);
                ++c;
                break;
            case 1:
                invalidate();
                break;
        }
    }

    public void invalidate() {
        if(valid) {
            valid = false;
            task.cancel();
            flashedCards.forEach(Entity::remove);
            if(onInvalidation != null)
                onInvalidation.call();
        }
    }
}
