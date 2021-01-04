package com.knoban.ultimates.rewards;

import com.knoban.ultimates.aspects.Items;
import com.knoban.ultimates.cardholder.Holder;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public abstract class Reward {

    protected final RewardInfo info = getClass().getAnnotation(RewardInfo.class);

    protected ItemStack icon;
    protected Tier tier;
    protected long amount;

    protected Reward(Map<String, Object> data) {
        this.icon = Items.DEFAULT_REWARD_ITEM;
        String tierName = (String) data.get("tier");
        if(tierName != null)
            this.tier = Tier.valueOf(tierName);
        this.amount = (long) data.getOrDefault("amount", 1L);
        if(amount < 0)
            amount = 0;
    }

    public Reward(ItemStack icon, @Nullable Tier tier, long amount) {
        this.icon = icon;
        this.tier = tier;
        this.amount = amount;
    }

    public RewardInfo getInfo() {
        return info;
    }

    public ItemStack getIcon() {
        return icon;
    }

    public void setIcon(ItemStack icon) {
        this.icon = icon;
    }

    @Nullable
    public Tier getTier() {
        return tier;
    }

    public void setTier(@Nullable Tier tier) {
        this.tier = tier;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public abstract void reward(Holder holder);

    public static Reward fromData(@NotNull Map<String, Object> data) throws Exception {
        String type = (String) data.get("type");
        if(type == null)
            throw new IllegalArgumentException("Missing reward type.");

        Class<? extends Reward> rewardClass = Rewards.getInstance().getRewardByName().get(type);
        if(rewardClass == null)
            throw new IllegalArgumentException("Invalid reward type: " + type);

        return rewardClass.getConstructor(Map.class).newInstance(data);
    }
}
