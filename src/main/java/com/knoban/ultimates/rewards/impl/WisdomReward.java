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

@RewardInfo(name = "wisdom")
public class WisdomReward extends Reward {

    public WisdomReward(Map<String, Object> data) {
        super(data);
        this.icon = createIcon(amount);
    }

    public WisdomReward(ItemStack icon, Tier tier, long amount) {
        super(icon, tier, amount);
    }

    public WisdomReward(Tier tier, long amount) {
        super(createIcon(amount), tier, amount);
    }

    private static ItemStack createIcon(long amount) {
        ItemStack icon = new ItemStack(Material.ENCHANTING_TABLE);
        ItemMeta wisdomIM = icon.getItemMeta();
        wisdomIM.setDisplayName("§5" + amount + " Wisdom");
        wisdomIM.setLore(Arrays.asList("§7Used to §9buy cards§7."));
        icon.setItemMeta(wisdomIM);
        return icon;
    }

    @Override
    public void reward(Holder holder) {
        Player p = Bukkit.getPlayer(holder.getUniqueId());
        if(p != null) {
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 0.6F);
            p.sendMessage("§2You got §b" + amount + " wisdom§2!");
        }

        holder.incrementWisdom((int) amount);
    }
}
