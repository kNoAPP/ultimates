package com.knoban.ultimates.rewards;

import com.knoban.ultimates.cardholder.Holder;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.inventory.ItemStack;

public interface SpecificReward {

    ItemStack getIcon(Holder holder);
    Tier getTier(Holder holder);
    long getAmount(Holder holder);
}
