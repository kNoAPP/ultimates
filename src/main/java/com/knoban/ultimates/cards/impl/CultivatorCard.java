package com.knoban.ultimates.cards.impl;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Fire;
import org.bukkit.block.data.type.Sapling;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;

@CardInfo(
        material = Material.BIRCH_SAPLING,
        name = "cultivators-touch",
        display = "§6Cultivator's Touch", // Typically we want the color to match the Primal
        description = {"§7Crops and overworld saplings you", "§6place §7age halfway instantly."},
        source = PrimalSource.OCEAN,
        tier = Tier.EPIC
)
public class CultivatorCard extends Card {

    public CultivatorCard(Ultimates plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSeed(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if(drawn.contains(p)) {
            Block placed = e.getBlockPlaced();
            BlockData data = placed.getBlockData();
            if(data instanceof Sapling) {
                Sapling sap = (Sapling) data;
                sap.setStage(sap.getMaximumStage()/2);
                placed.setBlockData(sap);
            } else if(data instanceof Ageable && !(data instanceof Fire)) {
                Ageable ageable = (Ageable) data;
                ageable.setAge(ageable.getMaximumAge()/2);
                placed.setBlockData(ageable);
                placed.getWorld().playSound(placed.getLocation(), Sound.ENTITY_COD_HURT, 1F, 1.7F);
            }
        }
    }

    // Grow all crops/saplings instantly. No issues, just needed to nerf the card.
    /*
    private void trySpawnTree(Block where, Material sapling, TreeType tree) {
        if(where.equals(where.getWorld().getHighestBlockAt(where.getLocation()))) {
            where.setType(Material.AIR);
            if(!where.getWorld().generateTree(where.getLocation(), tree)) {
                where.setType(sapling);
                where.getWorld().spawnParticle(Particle.SMOKE_NORMAL, where.getLocation().clone().add(0.5, 0.5, 0.5), 8, 0.5F, 0.5F, 0.5F, 0.01);
            } else
                where.getWorld().playSound(where.getLocation(), Sound.ENTITY_COD_HURT, 1F, 1.7F);
        } else where.getWorld().spawnParticle(Particle.SMOKE_NORMAL, where.getLocation().clone().add(0.5, 0.5, 0.5), 8, 0.5F, 0.5F, 0.5F, 0.01);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSeed(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if(drawn.contains(p)) {
            ItemStack inhand = e.getItemInHand();
            Block placed = e.getBlockPlaced();
            BlockData data = placed.getBlockData();
            if(data instanceof Sapling) {
                switch(inhand.getType()) {
                    case ACACIA_SAPLING:
                        trySpawnTree(placed, Material.ACACIA_SAPLING, TreeType.ACACIA);
                        break;
                    case SPRUCE_SAPLING:
                        trySpawnTree(placed, Material.SPRUCE_SAPLING, TreeType.REDWOOD);
                        break;
                    case BIRCH_SAPLING:
                        trySpawnTree(placed, Material.BIRCH_SAPLING, TreeType.BIRCH);
                        break;
                    case DARK_OAK_SAPLING:
                        trySpawnTree(placed, Material.DARK_OAK_SAPLING, TreeType.DARK_OAK);
                        break;
                    case JUNGLE_SAPLING:
                        trySpawnTree(placed, Material.JUNGLE_SAPLING, TreeType.JUNGLE);
                        break;
                    case OAK_SAPLING:
                        trySpawnTree(placed, Material.OAK_SAPLING, TreeType.TREE);
                        break;
                    default:
                        break;
                }
            } else if(inhand.getType() == Material.SUGAR_CANE) {
                Block next = placed.getWorld().getBlockAt(placed.getX(), placed.getY()+1, placed.getZ());
                int i;
                for(i=0; i<2 && next.getType() == Material.AIR; ++i) {
                    next.setType(Material.SUGAR_CANE);
                    next = next.getWorld().getBlockAt(next.getX(), next.getY()+1, next.getZ());
                }
                if(i>0)
                    placed.getWorld().playSound(placed.getLocation(), Sound.ENTITY_COD_HURT, 1F, 1.7F);
            } else if(data instanceof Ageable && !(data instanceof Fire)) {
                Ageable ageable = (Ageable) data;
                ageable.setAge(ageable.getMaximumAge());
                placed.setBlockData(ageable);
                placed.getWorld().playSound(placed.getLocation(), Sound.ENTITY_COD_HURT, 1F, 1.7F);
            } else if(data instanceof SeaPickle) {
                SeaPickle pickle = (SeaPickle) data;
                pickle.setPickles(pickle.getMaximumPickles());
                placed.setBlockData(pickle);
                placed.getWorld().playSound(placed.getLocation(), Sound.ENTITY_COD_HURT, 1F, 1.7F);
            } else if(inhand.getType() == Material.CHORUS_PLANT) {
                trySpawnTree(placed, Material.CHORUS_PLANT, TreeType.CHORUS_PLANT);
            } else if(inhand.getType() == Material.BAMBOO) {
                Block next = placed.getWorld().getBlockAt(placed.getX(), placed.getY()+1, placed.getZ());
                int i;
                for(i=0; i<6 && next.getType() == Material.AIR; ++i) {
                    next.setType(Material.BAMBOO);
                    next = next.getWorld().getBlockAt(next.getX(), next.getY()+1, next.getZ());
                }
                if(i>0)
                    placed.getWorld().playSound(placed.getLocation(), Sound.ENTITY_COD_HURT, 1F, 1.7F);
                for(int j=0; j<3 && i>0; ++j) {
                    next = next.getWorld().getBlockAt(next.getX(), next.getY()-1, next.getZ());
                    --i;
                    Bamboo bamboo = (Bamboo) next.getBlockData();
                    bamboo.setLeaves(j < 2 ? Bamboo.Leaves.LARGE : Bamboo.Leaves.SMALL);
                    next.setBlockData(bamboo);
                }
            } else if(inhand.getType() == Material.BROWN_MUSHROOM) {
                trySpawnTree(placed, Material.BROWN_MUSHROOM, TreeType.BROWN_MUSHROOM);
            } else if(inhand.getType() == Material.RED_MUSHROOM) {
                trySpawnTree(placed, Material.RED_MUSHROOM, TreeType.RED_MUSHROOM);
            }
        }
    }
    */
}
