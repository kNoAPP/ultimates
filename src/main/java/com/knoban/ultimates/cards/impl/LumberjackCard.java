package com.knoban.ultimates.cards.impl;

import com.knoban.atlas.world.Coordinate;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

@CardInfo(
        material = Material.IRON_AXE,
        name = "lumberjack",
        display = "§7Lumberjack", // Typically we want the color to match the Primal
        description = {"§7Your axes fell trees §ainstantly§7."},
        source = PrimalSource.NONE,
        tier = Tier.COMMON
)
public class LumberjackCard extends Card {

    private static final EnumSet<Material> LOGS = EnumSet.of(
            Material.ACACIA_LOG, Material.BIRCH_LOG, Material.DARK_OAK_LOG, Material.JUNGLE_LOG, Material.OAK_LOG,
            Material.SPRUCE_LOG, Material.STRIPPED_ACACIA_LOG, Material.STRIPPED_BIRCH_LOG,
            Material.STRIPPED_DARK_OAK_LOG, Material.STRIPPED_JUNGLE_LOG, Material.STRIPPED_OAK_WOOD,
            Material.STRIPPED_SPRUCE_LOG, Material.CRIMSON_STEM, Material.STRIPPED_CRIMSON_STEM,
            Material.WARPED_STEM, Material.STRIPPED_WARPED_STEM
    );

    private static final EnumSet<Material> LEAVES = EnumSet.of(
            Material.ACACIA_LEAVES, Material.BIRCH_LEAVES, Material.DARK_OAK_LEAVES, Material.JUNGLE_LEAVES,
            Material.OAK_LEAVES, Material.SPRUCE_LEAVES, Material.NETHER_WART_BLOCK, Material.WARPED_WART_BLOCK
    );

    private static final EnumSet<Material> AXES = EnumSet.of(
            Material.NETHERITE_AXE, Material.DIAMOND_AXE, Material.GOLDEN_AXE, Material.IRON_AXE, Material.STONE_AXE, Material.WOODEN_AXE
    );

    public LumberjackCard(Ultimates plugin) {
        super(plugin);
    }

    // We use this private HashSet to avoid an infinite loop from calling a BlockBreakEvent for compatibility
    private HashSet<Player> eventHandling = new HashSet<Player>();
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTreeFell(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if(drawn.contains(p) && !eventHandling.contains(p)) {
            ItemStack is = p.getInventory().getItemInMainHand();
            Block b = e.getBlock();
            if(is != null && AXES.contains(is.getType()) && isTree(b)) {
                ArrayList<Block> logsToDestroy = getTreeWood(b);
                e.setCancelled(true);
                e.setDropItems(false);

                eventHandling.add(p);
                new BukkitRunnable() {
                    int index = 0;

                    public void run() {
                        if(p.isOnline() && p.isValid() && index != logsToDestroy.size()) {
                            Block log = logsToDestroy.get(index);
                            BlockBreakEvent bbe = new BlockBreakEvent(log, p);
                            plugin.getServer().getPluginManager().callEvent(bbe);
                            if(!bbe.isCancelled()) {
                                log.getWorld().playEffect(log.getLocation(), Effect.STEP_SOUND, log.getType());
                                log.breakNaturally();
                            }
                            ++index;
                        } else {
                            eventHandling.remove(p);
                            this.cancel();
                        }
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }
        }
    }

    private ArrayList<Block> getTreeWood(Block chopped) {
        if(!LOGS.contains(chopped.getType())) {
            return new ArrayList<Block>();
        }

        ArrayList<Block> wood = new ArrayList<>();
        HashSet<Coordinate> visited = new HashSet<>();
        Queue<Block> toVisit = new LinkedList<>();

        toVisit.add(chopped);
        visited.add(new Coordinate(chopped.getLocation()));

        while(!toVisit.isEmpty()) {
            Block visiting = toVisit.poll();
            wood.add(visiting);

            for(BlockFace bf : BlockFace.values()) {
                Block possibleWood = visiting.getRelative(bf);
                Coordinate cord = new Coordinate(possibleWood.getLocation());
                if(LOGS.contains(possibleWood.getType()) && !visited.contains(cord)) {
                    toVisit.offer(possibleWood);
                    visited.add(cord);
                }
            }
        }

        return wood;
    }

    private boolean isTree(Block chopped) {
        if(!LOGS.contains(chopped.getType()))
            return false;

        if(numTrue(LEAVES.contains(chopped.getRelative(BlockFace.UP).getType()),
                LEAVES.contains(chopped.getRelative(BlockFace.NORTH).getType()),
                LEAVES.contains(chopped.getRelative(BlockFace.EAST).getType()),
                LEAVES.contains(chopped.getRelative(BlockFace.SOUTH).getType()),
                LEAVES.contains(chopped.getRelative(BlockFace.WEST).getType())) >= 4)
            return true;


        Block check = chopped.getRelative(BlockFace.UP);
        return isTree(check) ||
                isTree(check.getRelative(BlockFace.NORTH)) ||
                isTree(check.getRelative(BlockFace.EAST)) ||
                isTree(check.getRelative(BlockFace.SOUTH)) ||
                isTree(check.getRelative(BlockFace.WEST));
    }

    private int numTrue(boolean... values) {
        int num = 0;
        for(boolean b : values) {
            if(b)
                num++;
        }

        return num;
    }
}
