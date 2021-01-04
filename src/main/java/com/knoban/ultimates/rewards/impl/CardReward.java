package com.knoban.ultimates.rewards.impl;

import com.knoban.ultimates.cardholder.Holder;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.Cards;
import com.knoban.ultimates.primal.Tier;
import com.knoban.ultimates.rewards.Reward;
import com.knoban.ultimates.rewards.RewardInfo;
import com.knoban.ultimates.rewards.SpecificReward;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.TreeMap;

@RewardInfo(name = "card")
public class CardReward extends Reward implements SpecificReward {

    private Card card;
    private Reward backup;

    public CardReward(Map<String, Object> data) throws Exception {
        super(data);

        String cardName = (String) data.getOrDefault("card", "");
        this.card = Cards.getInstance().getCardInstance(cardName);
        if(card == null)
            throw new IllegalArgumentException("Invalid card: " + cardName);
        this.icon = card.getUnownedIcon();
        this.backup = Reward.fromData((Map<String, Object>) data.getOrDefault("backup", new TreeMap<>()));
    }

    public CardReward(Card card, @Nullable Reward backup) {
        super(card.getUnownedIcon(), card.getInfo().tier(), 1);
        this.card = card;
        this.backup = backup;
    }

    public Card getCard() {
        return card;
    }

    public void setCard(Card card) {
        this.card = card;
    }

    public Reward getBackup() {
        return backup;
    }

    public void setBackup(Reward backup) {
        this.backup = backup;
    }

    @Override
    public ItemStack getIcon(Holder holder) {
        if(backup == null || !holder.getOwnedCards().contains(card))
            return icon.clone();

        return backup.getIcon();
    }

    @Override
    public Tier getTier(Holder holder) {
        if(backup == null || !holder.getOwnedCards().contains(card))
            return tier;

        return backup.getTier();
    }

    @Override
    public long getAmount(Holder holder) {
        if(backup == null || !holder.getOwnedCards().contains(card))
            return amount;

        return backup.getAmount();
    }

    @Override
    public void reward(Holder holder) {
        if(!holder.grantCards(card) && backup != null) {
            backup.reward(holder);
            return;
        }

        Player p = Bukkit.getPlayer(holder.getUniqueId());
        if(p != null) {
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7F, 0.8F);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5F, 1F);
            p.sendMessage("§2You've unlocked a new card: " + card.getInfo().display());
        }
    }
}
