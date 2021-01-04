package com.knoban.ultimates.cardpack.animations;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cardholder.CardHolder;
import com.knoban.ultimates.cardpack.CardPack;
import com.knoban.ultimates.rewards.Reward;
import org.bukkit.entity.Player;

public abstract class CardPackAnimation {

    protected final Ultimates plugin;

    protected final Player awardTo;
    protected final CardHolder rewardTo;
    protected final CardPack opening;

    protected boolean willAward;
    protected Reward[] rewards;

    public CardPackAnimation(Ultimates plugin, Player awardTo, CardPack opening) {
        this(plugin, awardTo, opening, true);
    }

    public CardPackAnimation(Ultimates plugin, Player awardTo, CardPack opening, boolean willAward) {
        this.plugin = plugin;
        this.awardTo = awardTo;
        this.rewardTo = CardHolder.getCardHolder(awardTo);
        this.opening = opening;
        this.willAward = willAward;
        this.rewards = new Reward[]{};
    }

    public abstract void play();

    public boolean willAward() {
        return willAward;
    }

    public void setWillAward(boolean willAward) {
        this.willAward = willAward;
    }

    public Reward[] getRewards() {
        return rewards;
    }
}
