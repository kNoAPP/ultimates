package com.knoban.ultimates.cards.impl;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.knoban.atlas.utils.Cooldown;
import com.knoban.atlas.utils.Tools;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.TimeUnit;

@CardInfo(
        material = Material.DIAMOND,
        name = "xray",
        display = "§aX-Ray", // Typically we want the color to match the Primal
        description = {"§9Right-Click §7your pickaxe to", "§7uncover §brare ores §7around you."},
        source = PrimalSource.EARTH,
        tier = Tier.LEGENDARY
)
public class XRayCard extends Card {

    private static final int COOLDOWN_SECONDS = 45;
    private static final int MAX_VEIN_SIZE = 20;
    private static final int REVEALED_SECONDS = 15;
    private static final String METADATA = "ults_xray_shulker";

    private static final EnumSet<Material> PICKAXES = EnumSet.of(
            Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.GOLDEN_PICKAXE, Material.IRON_PICKAXE,
            Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE
    );
    private static final EnumSet<Material> ORES = EnumSet.of(
            Material.COAL_ORE, Material.IRON_ORE, Material.GOLD_ORE, Material.REDSTONE_ORE,
            Material.DIAMOND_ORE, Material.ANCIENT_DEBRIS, Material.LAPIS_ORE, Material.EMERALD_ORE
    );

    private Cache<UUID, Cooldown> cooldowns = CacheBuilder.newBuilder()
            .expireAfterWrite(COOLDOWN_SECONDS, TimeUnit.SECONDS).build();
    private HashMap<Block, Shulker> shulkerBlocks = new HashMap<>();

    public XRayCard(Ultimates plugin) {
        super(plugin);

        for(World w : Bukkit.getWorlds()) {
            for(Entity e : w.getEntities()) {
                if(e.hasMetadata(METADATA))
                    e.remove();
            }
        }
    }

    private static int rangeForPickaxe(Material type) {
        switch(type) {
            case WOODEN_PICKAXE:
                return 3;
            case STONE_PICKAXE:
                return 5;
            case IRON_PICKAXE:
                return 8;
            case GOLDEN_PICKAXE:
                return 9;
            case DIAMOND_PICKAXE:
                return 10;
            case NETHERITE_PICKAXE:
                return 12;
            default:
                return 0;
        }
    }

    @Override
    protected void unregister() {
        super.unregister();
        shulkerBlocks.forEach((b, s) -> s.remove());
        shulkerBlocks.clear();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if(!drawn.contains(p))
            return;

        if(e.getHand() != EquipmentSlot.HAND)
            return;

        Action action = e.getAction();
        if(action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack is = e.getItem();
        int range;
        if(is != null && (range = rangeForPickaxe(is.getType())) > 0) {
            Cooldown cooldown = cooldowns.getIfPresent(p.getUniqueId());
            if(cooldown != null) {
                p.sendMessage(info.display() + " §cis on cooldown for another " + cooldown.toTimestampString() + ".");
                return;
            }

            cooldowns.put(p.getUniqueId(), new Cooldown(COOLDOWN_SECONDS*1000));
            p.sendMessage("§8With a §b" + Tools.enumNameToHumanReadable(is.getType().name()) + "§8, you reveal an "
                + "ore within §e" + range + " blocks§8!");
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GUARDIAN_DEATH, 1F, 0.8F);
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                Block ore = findNearestOre(p.getLocation().getBlock(), range*range*range);
                if(ore == null) {
                    p.sendMessage("§cBut unfortunately, there are no ores to be found.");
                    return;
                }

                p.sendMessage("§2Ah, victory. §7A vein has been revealed.");
                Set<Block> vein = getVein(ore, MAX_VEIN_SIZE);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    for(Block b : vein) {
                        Shulker shulker = b.getWorld().spawn(b.getLocation(), Shulker.class);
                        shulker.setAI(false);
                        shulker.setInvulnerable(true);
                        shulker.setAware(false);
                        shulker.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20*REVEALED_SECONDS,
                                0, true, false));
                        shulker.setGlowing(true);
                        shulker.setCollidable(false);
                        shulker.setMetadata(METADATA, new FixedMetadataValue(plugin, true));
                        shulkerBlocks.put(b, shulker);
                    }

                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        vein.forEach((b) -> {
                            Shulker shulker = shulkerBlocks.remove(b);
                            if(shulker != null)
                                shulker.remove();
                        });
                    }, REVEALED_SECONDS*20L);
                });
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOreBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        Shulker shulker = shulkerBlocks.remove(b);
        if(shulker != null)
            shulker.remove();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOreExplode(BlockExplodeEvent e) {
        for(Block b : e.blockList()) {
            Shulker shulker = shulkerBlocks.remove(b);
            if(shulker != null)
                shulker.remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOreExplode(BlockDestroyEvent e) {
        Block b = e.getBlock();
        Shulker shulker = shulkerBlocks.remove(b);
        if(shulker != null)
            shulker.remove();
    }

    private static final List<BlockFace> SEARCH_DIRECTIONS = Arrays.asList(
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
    );

    /**
     * This is the most efficient algorithm I have the brain power to make for this function. I'm
     * aware an expanding spherical search would be faster, but I'm clueless on the calculus needed
     * to make that work.
     *
     * If Gere or someone want's to improve this, be my guest.
     */
    private static Block findNearestOre(Block origin, int numSearchBlocks) {
        if(ORES.contains(origin.getType()))
            return origin;

        HashSet<Block> visited = new HashSet<>();
        Queue<Block> toVisit = new LinkedList<>();
        toVisit.offer(origin);
        visited.add(origin);
        while(!toVisit.isEmpty() && numSearchBlocks > 0) {
            Block b = toVisit.poll();
            if(ORES.contains(b.getType()))
                return b;

            for(BlockFace search : SEARCH_DIRECTIONS) {
                Block toSearch = b.getRelative(search);
                if(visited.add(toSearch))
                    toVisit.offer(toSearch);
            }

            --numSearchBlocks;
        }

        return null;
    }

    private static Set<Block> getVein(Block from, int maxVeinSize) {
        Material type = from.getType();
        HashSet<Block> vein = new HashSet<>();
        Queue<Block> toVisit = new LinkedList<>();
        toVisit.offer(from);
        vein.add(from);
        while(!toVisit.isEmpty() && vein.size() < maxVeinSize) {
            Block b = toVisit.poll();
            for(BlockFace search : SEARCH_DIRECTIONS) {
                Block searchedBlock = b.getRelative(search);
                if(searchedBlock.getType() == type && vein.add(searchedBlock))
                    toVisit.add(searchedBlock);
            }
        }

        return vein;
    }
}
