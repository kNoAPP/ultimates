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

@RewardInfo(name = "xp")
public class ExperienceReward extends Reward {

    public ExperienceReward(Map<String, Object> data) {
        super(data);
        this.icon = createIcon(amount);
    }

    public ExperienceReward(ItemStack icon, Tier tier, long amount) {
        super(icon, tier, amount);
    }

    public ExperienceReward(Tier tier, long amount) {
        super(createIcon(amount), tier, amount);
    }

    private static ItemStack createIcon(long amount) {
        ItemStack icon = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta im = icon.getItemMeta();
        im.setDisplayName("§5" + amount + " xp");
        im.setLore(Arrays.asList("§7Levels up the §6Battle Pass§7!"));
        icon.setItemMeta(im);
        return icon;
    }

    @Override
    public void reward(Holder holder) {
        Player p = Bukkit.getPlayer(holder.getUniqueId());
        if(p != null) {
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 0.6F);
            p.sendMessage("§aYou got §b" + amount + " xp§a!");
        }

        holder.incrementXp((int) amount);
    }
}
