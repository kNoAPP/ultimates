package com.knoban.ultimates.rewards;

import com.knoban.atlas.rewards.Reward;
import com.knoban.atlas.rewards.RewardInfo;
import com.knoban.ultimates.cardholder.CardHolder;
import com.knoban.ultimates.cardholder.Holder;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.Cards;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.TreeMap;

@RewardInfo(name = "card")
public class CardReward extends Reward {

    private Card card;
    private Reward backup;

    public CardReward(Map<String, Object> data) throws Exception {
        super(data);

        String cardName = (String) data.getOrDefault("card", "");
        this.card = Cards.getInstance().getCardInstanceByName(cardName);
        if(card == null)
            throw new IllegalArgumentException("Invalid card: " + cardName);
        this.icon = card.getUnownedIcon();
        this.backup = Reward.fromData((Map<String, Object>) data.getOrDefault("backup", new TreeMap<>()));
    }

    public CardReward(Card card, @Nullable Reward backup) {
        super(card.getUnownedIcon(), 1);
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

    public ItemStack getIcon(@NotNull Holder holder) {
        if(backup == null || !holder.getOwnedCards().contains(card))
            return icon.clone();

        return backup.getIcon();
    }

    public long getAmount(@NotNull Holder holder) {
        if(backup == null || !holder.getOwnedCards().contains(card))
            return amount;

        return backup.getAmount();
    }

    @Override
    public void reward(@NotNull Player p) {
        CardHolder holder = CardHolder.getCardHolder(p);
        if(holder != null && holder.isLoaded() && !holder.grantCards(card) && backup != null) {
            backup.reward(p);
            return;
        }

        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7F, 0.8F);
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5F, 1F);
        p.sendMessage("§2You've unlocked a new card: " + card.getInfo().display());
    }
}
