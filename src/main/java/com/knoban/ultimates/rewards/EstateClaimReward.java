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

@RewardInfo(name = "estate-claim")
public class EstateClaimReward extends Reward {

    public EstateClaimReward(Map<String, Object> data) {
        super(data);
        this.icon = createIcon(amount);
    }

    public EstateClaimReward(ItemStack icon, long amount) {
        super(icon, amount);
    }

    public EstateClaimReward(long amount) {
        super(createIcon(amount), amount);
    }

    private static ItemStack createIcon(long amount) {
        ItemStack icon = new ItemStack(Material.CAMPFIRE);
        ItemMeta wisdomIM = icon.getItemMeta();
        wisdomIM.displayName(Component.text("§3" + amount + " Extra Estate" + (amount == 1 ? "" : "s")));
        wisdomIM.lore(Arrays.asList(Component.text("§7Used to §9claim §7chunks of land.")));
        icon.setItemMeta(wisdomIM);
        return icon;
    }

    @Override
    public void reward(@NotNull Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST_FAR, 3F, 0.8F);
        p.playSound(p.getLocation(), Sound.ITEM_TRIDENT_RETURN, 3F, 0.5F);
        p.sendMessage("§2You got an extra §3" + amount + " Estate Claim" + (amount == 1 ? "" : "s") + "§2!");
        p.sendMessage("§7Claim more land with §e/estate claim§7.");

        CardHolder holder = CardHolder.getCardHolder(p);
        if(holder != null && holder.isLoaded())
            holder.incrementMaxEstateClaims((int) amount);
    }
}
