package com.knoban.ultimates.cards.impl;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

@CardInfo(
        material = Material.DARK_OAK_SAPLING,
        name = "luck-of-the-gods",
        display = "§7Luck of the Gods", // Typically we want the color to match the Primal
        description = {"§7You become very §alucky§7,", "§eincreasing§7 your chances of",
                "§7getting §ebetter§7 loot from", "chests and fishing"},
        source = PrimalSource.OCEAN,
        tier = Tier.RARE
)
public class LuckCard extends Card {

    public LuckCard(Ultimates plugin) {
        super(plugin);
    }

    @Override
    public boolean draw(Player p) {
        boolean didEquip = super.draw(p);
        if(didEquip) {
            p.getAttribute(Attribute.LUCK).setBaseValue(15);
        }
        return didEquip;
    }

    @Override
    public boolean discard(Player p) {
        boolean didDiscard = super.discard(p);
        if(didDiscard) {
            p.getAttribute(Attribute.LUCK).setBaseValue(p.getAttribute(Attribute.LUCK).getDefaultValue());
        }
        return didDiscard;
    }
}