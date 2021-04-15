package com.knoban.ultimates.rewards;

import com.knoban.atlas.rewards.Reward;
import com.knoban.atlas.rewards.RewardInfo;
import com.knoban.ultimates.cardholder.CardHolder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;

@RewardInfo(name = "wisdom")
public class WisdomReward extends Reward {

    public WisdomReward(Map<String, Object> data) {
        super(data);
        this.icon = createIcon(amount);
    }

    public WisdomReward(ItemStack icon, long amount) {
        super(icon, amount);
    }

    public WisdomReward(long amount) {
        super(createIcon(amount), amount);
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
    public void reward(@NotNull Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 0.6F);
        p.sendMessage("§2You got §b" + amount + " wisdom§2!");


        CardHolder holder = CardHolder.getCardHolder(p);
        if(holder != null && holder.isLoaded())
            holder.incrementWisdom((int) amount);
    }
}
