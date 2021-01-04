package com.knoban.ultimates.cards.impl;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

@CardInfo(
        material = Material.IRON_BARS,
        name = "tank-armor",
        display = "§9Tank Armor", // Typically we want the color to match the Primal
        description = {"§7You have 6 extra max hp."},
        source = PrimalSource.NONE,
        tier = Tier.COMMON
)
public class TankCard extends Card {

    public TankCard(Ultimates plugin) {
        super(plugin);
    }

    @Override
    public boolean draw(Player p) {
        boolean didEquip = super.draw(p);
        if(didEquip) {
            AttributeInstance maxHealth = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if(maxHealth != null)
                maxHealth.setBaseValue(maxHealth.getDefaultValue() + 6);
        }
        return didEquip;
    }

    @Override
    public boolean discard(Player p) {
        boolean didDispose = super.discard(p);
        if(didDispose) {
            AttributeInstance maxHealth = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if(maxHealth != null)
                maxHealth.setBaseValue(maxHealth.getDefaultValue());
        }
        return didDispose;
    }
}
