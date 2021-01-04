package com.knoban.ultimates.rewards.impl;

import com.knoban.ultimates.cardholder.Holder;
import com.knoban.ultimates.primal.Tier;
import com.knoban.ultimates.rewards.Reward;
import com.knoban.ultimates.rewards.RewardInfo;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Map;

@RewardInfo(name = "card-slot")
public class CardSlotReward extends Reward {

    public CardSlotReward(Map<String, Object> data) {
        super(data);
        this.icon = createIcon(amount);
    }

    public CardSlotReward(ItemStack icon, Tier tier, long amount) {
        super(icon, tier, amount);
    }

    public CardSlotReward(Tier tier, long amount) {
        super(createIcon(amount), tier, amount);
    }

    private static ItemStack createIcon(long amount) {
        ItemStack icon = new ItemStack(Material.MUSIC_DISC_CHIRP);
        ItemMeta wisdomIM = icon.getItemMeta();
        wisdomIM.setDisplayName("§c" + amount + " Extra Card Slot" + (amount == 1 ? "" : "s"));
        wisdomIM.setLore(Arrays.asList("§7Increases the number of cards", "§7you can have drawn at once."));
        icon.setItemMeta(wisdomIM);
        return icon;
    }

    @Override
    public void reward(Holder holder) {
        Player p = Bukkit.getPlayer(holder.getUniqueId());
        if(p != null) {
            p.playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 3F, 0.8F);
            p.playSound(p.getLocation(), Sound.BLOCK_BELL_RESONATE, 3F, 0.3F);
            p.sendMessage("§2You got an extra §c" + amount + " Card Slot" + (amount == 1 ? "" : "s") + "§2!");
            p.sendMessage("§7You can now draw more cards! Draw some with §e/card menu§7.");
        }

        holder.incrementMaxCardSlots((int) amount);
    }
}
