package com.knoban.ultimates.cards;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.aspects.Items;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

import java.util.EnumSet;

public class GeneralCardListener implements Listener {

    private Ultimates plugin;

    public GeneralCardListener(Ultimates plugin) {
        this.plugin = plugin;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * This whole mess is just to keep items with a
     * locked lore identifier from being moved out of a Player's
     * Inventory.
     */
    private static final EnumSet<ClickType> disabledTwoInv = EnumSet.of(ClickType.CONTROL_DROP,
            ClickType.DOUBLE_CLICK, ClickType.DROP, ClickType.NUMBER_KEY, ClickType.SHIFT_LEFT, ClickType.SHIFT_RIGHT,
            ClickType.UNKNOWN, ClickType.WINDOW_BORDER_LEFT, ClickType.WINDOW_BORDER_RIGHT);

    private static final EnumSet<ClickType> disabledOneInv = EnumSet.of(ClickType.CONTROL_DROP,
            ClickType.DROP, ClickType.NUMBER_KEY, ClickType.UNKNOWN, ClickType.WINDOW_BORDER_LEFT,
            ClickType.WINDOW_BORDER_RIGHT);
    @EventHandler
    public void onInventoryMove(InventoryClickEvent e) {
        if(Items.isLocked(e.getCurrentItem())) {
            if(e.getInventory() != null) {
                if(disabledTwoInv.contains(e.getClick())) {
                    e.setCancelled(true);
                    return;
                }
            } else if(disabledOneInv.contains(e.getClick())) {
                e.setCancelled(true);
                return;
            }
        }
        if(!e.getWhoClicked().getInventory().equals(e.getClickedInventory())) {
            if(Items.isLocked(e.getCursor())) {
                e.setCancelled(true);
                return;
            }
            if((e.getAction() == InventoryAction.HOTBAR_SWAP || e.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD)
                    && e.getClick() == ClickType.NUMBER_KEY
                    && Items.isLocked(e.getWhoClicked().getInventory().getItem(e.getHotbarButton()))) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if(Items.isLocked(e.getItemDrop().getItemStack()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onEnchant(PrepareItemEnchantEvent e) {
        if(Items.isLocked(e.getItem()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent e) {
        if(Items.isLocked(e.getItem()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        e.getDrops().removeIf((i) -> !Items.isLocked(i));
    }

    /* END OF MESS */
}
