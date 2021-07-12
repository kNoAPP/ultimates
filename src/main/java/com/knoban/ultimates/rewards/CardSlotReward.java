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

@RewardInfo(name = "card-slot")
public class CardSlotReward extends Reward {

    public CardSlotReward(Map<String, Object> data) {
        super(data);
        this.icon = createIcon(amount);
    }

    public CardSlotReward(ItemStack icon, long amount) {
        super(icon, amount);
    }

    public CardSlotReward(long amount) {
        super(createIcon(amount), amount);
    }

    private static ItemStack createIcon(long amount) {
        ItemStack icon = new ItemStack(Material.MUSIC_DISC_CHIRP);
        ItemMeta wisdomIM = icon.getItemMeta();
        wisdomIM.displayName(Component.text("§c" + amount + " Extra Card Slot" + (amount == 1 ? "" : "s")));
        wisdomIM.lore(Arrays.asList(Component.text("§7Increases the number of cards"), Component.text("§7you can have drawn at once.")));
        icon.setItemMeta(wisdomIM);
        return icon;
    }

    @Override
    public void reward(@NotNull Player p) {
        p.playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 3F, 0.8F);
        p.playSound(p.getLocation(), Sound.BLOCK_BELL_RESONATE, 3F, 0.3F);
        p.sendMessage("§2You got an extra §c" + amount + " Card Slot" + (amount == 1 ? "" : "s") + "§2!");
        p.sendMessage("§7You can now draw more cards! Draw some with §e/card menu§7.");

        CardHolder holder = CardHolder.getCardHolder(p);
        if(holder != null && holder.isLoaded())
            holder.incrementMaxCardSlots((int) amount);
    }
}
