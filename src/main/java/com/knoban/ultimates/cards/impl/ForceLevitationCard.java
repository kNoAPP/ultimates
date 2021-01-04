package com.knoban.ultimates.cards.impl;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.helpers.Levitation;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@CardInfo(
        material = Material.TORCH,
        name = "way-of-the-jedi",
        display = "§bWay of the Jedi", // Typically we want the color to match the Primal
        description = {"§7You can §blevitate §7blocks", "§7into the air!", "",
                "§eRight-click §7with your Levitation", "§7Wand to pick up something.", "",
                "§eLeft-click §7to launch it.", "", "§6Change item slots §7to reposition", "§7the block's distance."},
        source = PrimalSource.SKY,
        tier = Tier.LEGENDARY
)
public class ForceLevitationCard extends Card {

    /**
     * IF YOU ENCOUNTER PROBLEMS WITH SPAWN GRIEFING WITH THIS CARD...
     * Listen to the EntityChangeBlockEvent. When the falling block
     * lands, it triggers it.
     *
     * Check if EntityType == FALLING_BLOCK and cancel it.
     */

    public static final ItemStack LEVITATION_ITEM = getLevitationItem();
    public static final String METADATA = "ults_levitation_proj";
    private HashMap<Player, Levitation> levitators = new HashMap<>();

    public ForceLevitationCard(Ultimates plugin) {
        super(plugin);
    }

    @Override
    public boolean draw(Player p) {
        boolean didEquip = super.draw(p);
        if(didEquip) {
            p.getInventory().addItem(LEVITATION_ITEM);
        }
        return didEquip;
    }

    @Override
    public boolean discard(Player p) {
        boolean didDispose = super.discard(p);
        if(didDispose) {
            Levitation lev = levitators.get(p);
            if(lev != null)
                lev.drop();
            p.getInventory().removeItemAnySlot(LEVITATION_ITEM);
        }
        return didDispose;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRespawn(PlayerPostRespawnEvent e) {
        Player p = e.getPlayer();
        if(drawn.contains(p) && !p.getInventory().contains(LEVITATION_ITEM)) {
            p.getInventory().addItem(LEVITATION_ITEM);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockClick(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if(drawn.contains(p)) {
            ItemStack is = p.getInventory().getItemInMainHand();
            if(is != null && is.isSimilar(LEVITATION_ITEM)) {
                if(levitators.get(p) == null) {
                    Block b = e.getBlockAgainst();
                    if(b.getType() != Material.TORCH && b.getType() != Material.BEDROCK) { // Strange bug when picking up grass and flowers
                        Levitation lev = new Levitation(plugin, p, b, () -> levitators.remove(p));
                        levitators.put(p, lev);
                        lev.levitate();
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> b.setType(Material.AIR), 2L);
                    }
                }
                e.setCancelled(true);
            }
        }
    }

    // THIS WORKS FINE! I'm just commenting it out since SuplexCard needs to share some use.
    /*
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractAt(PlayerInteractAtEntityEvent e) {
        Player p = e.getPlayer();
        if(drawn.contains(p) && e.getHand() == EquipmentSlot.HAND) {
            Entity en = e.getRightClicked();
            ItemStack is = p.getInventory().getItemInMainHand();
            if(is != null && is.isSimilar(Items.LEVITATION_ITEM) && levitators.get(p) == null
                    && en instanceof LivingEntity && !(en instanceof Player)) {
                LivingEntity le = (LivingEntity) en;
                if(le.getVehicle() == null || !(le.getVehicle() instanceof ArmorStand)) {
                    Levitation lev = new Levitation(plugin, p, le, () -> levitators.remove(p));
                    levitators.put(p, lev);
                    lev.levitate();
                    e.setCancelled(true);
                }
            }
        }
    }
    */

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if(drawn.contains(p) && e.getHand() == EquipmentSlot.HAND) {
            Levitation lev = levitators.get(p);
            if(lev != null && e.getAction() != Action.PHYSICAL) {
                e.setCancelled(true);
                if(e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    lev.drop();
                } else if(e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK)
                    lev.launch();
            }
        }
    }

    /**
     * If this is firing before the InteractEvent down the road for
     * some reason, you'll get the levitation wand launching blocks
     * it should be dropping. You can fix this by making a BukkitTask
     * that runs 1 tick later in this method. Right after (lev != null).
     * Be sure to do an additional validity check on lev as well.
     *
     * Alternative, mount a slime of size 2 on the player's head
     * and check the EntityDamageByEntityEvent
     */
    @EventHandler
    public void onAnimation(PlayerAnimationEvent e) {
        Player p = e.getPlayer();
        if(drawn.contains(p)) {
            Levitation lev = levitators.get(p);
            if(lev != null)
                lev.launch();
        }
    }

    private static ItemStack getLevitationItem() {
        ItemStack is = new ItemStack(Material.TORCH, 1);
        ItemMeta im = is.getItemMeta();
        im.setDisplayName(ChatColor.AQUA + "Levitation Wand");
        List<String> lores = new ArrayList<>();
        lores.add(LOCKED_METADATA_LORE);
        lores.add("");
        lores.add("§aRight-click §7to pick up a block.");
        lores.add("§aRight-click §7to drop up a block.");
        lores.add("§eSwitch item slots §7to adjust levitation distance.");
        lores.add("§cLeft-click §7to launch your block.");
        im.setLore(lores);
        is.setItemMeta(im);
        return is;
    }
}
