package com.knoban.ultimates.rewards;

import com.knoban.atlas.rewards.Reward;
import com.knoban.atlas.rewards.RewardInfo;
import com.knoban.ultimates.cardholder.CardHolder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;

@RewardInfo(name = "xp")
public class ExperienceReward extends Reward {

    public ExperienceReward(Map<String, Object> data) {
        super(data);
        this.icon = createIcon(amount);
    }

    public ExperienceReward(ItemStack icon, long amount) {
        super(icon, amount);
    }

    public ExperienceReward(long amount) {
        super(createIcon(amount), amount);
    }

    private static ItemStack createIcon(long amount) {
        ItemStack icon = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta im = icon.getItemMeta();
        im.displayName(Component.text("§5" + amount + " xp"));
        im.lore(Arrays.asList(Component.text("§7Levels up the §6Battle Pass§7!")));
        icon.setItemMeta(im);
        return icon;
    }

    @Override
    public void reward(@NotNull Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 0.6F);
        p.sendMessage("§aYou got §b" + amount + " xp§a!");

        CardHolder holder = CardHolder.getCardHolder(p);
        if(holder != null && holder.isLoaded())
            holder.incrementXp((int) amount);
    }
}
