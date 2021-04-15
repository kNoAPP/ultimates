package com.knoban.ultimates.cardpack;

import com.knoban.atlas.rewards.Reward;
import com.knoban.atlas.utils.Tools;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.cardholder.CardHolder;
import com.knoban.ultimates.cardpack.animations.CardPackAnimation;
import com.knoban.ultimates.cardpack.animations.SpinnerCardPackAnimation;
import com.knoban.ultimates.cardpack.animations.TriSpinnerCardPackAnimation;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.Cards;
import com.knoban.ultimates.primal.Tier;
import com.knoban.ultimates.rewards.CardReward;
import com.knoban.ultimates.rewards.WisdomReward;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

public enum CardPack {

    // Always add new card packs to the end. Storage of these packs is dependent on ordering in this enum.
    // Never remove card packs once they've been added. Some players may have unclaimed packs!

    BASIC, EPIC, TRIPLE;

    private String name;

    CardPack() {
        this.name = Tools.enumNameToHumanReadable(name());
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public CardPackAnimation createDefaultAnimation(@NotNull Ultimates plugin, @NotNull Player awardTo) {
        return createDefaultAnimation(plugin, awardTo, true);
    }

    @NotNull
    public CardPackAnimation createDefaultAnimation(@NotNull Ultimates plugin, @NotNull Player awardTo, boolean award) {
        switch(this) {
            case BASIC:
                return new SpinnerCardPackAnimation(plugin, awardTo, this, award);
            case TRIPLE:
                return new TriSpinnerCardPackAnimation(plugin, awardTo, this, award);
            case EPIC:
                return new SpinnerCardPackAnimation(plugin, awardTo, this, award);
            default:
                return null;
        }
    }

    private static final Random rand = new Random();
    @NotNull
    public Reward getRandomReward(@NotNull CardHolder toReward) {
        Player showTo = toReward.getPlayer();
        double chance = rand.nextDouble();
        switch(this) {
            case TRIPLE:
            case BASIC:
                final int wisdomAmt = (int) (chance * 200) + 50;
                double sum = 0;
                for(Tier t : Tier.values()) {
                    sum += t.getChance();
                    if(chance <= sum) {
                        List<Card> tieredCards = Cards.getInstance().getCardInstancesFilterTier(t);
                        if(tieredCards.size() > 0) {
                            Card randomCard = tieredCards.get(rand.nextInt(tieredCards.size()));
                            if(!toReward.getOwnedCards().contains(randomCard)) {
                                return new CardReward(randomCard, null) {
                                    @Override
                                    public void reward(Player p) {
                                        if(toReward.getOwnedCards().contains(randomCard)) {
                                            showTo.playSound(showTo.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7F, 0.6F);
                                            showTo.sendMessage("§5You rolled a duplicate card! §bHere's " + wisdomAmt + " wisdom instead.");
                                            return;
                                        }
                                        super.reward(p);
                                    }
                                };
                            } else
                                break;
                        } else
                            break;
                    }
                }
                return new WisdomReward(wisdomAmt);
            case EPIC:
                final int wisdomEpicAmt = (int) (chance * 1000) + 500;
                List<Card> tieredCards = Cards.getInstance().getCardInstancesFilterTier(Tier.EPIC);
                if(tieredCards.size() > 0) {
                    Card randomCard = tieredCards.get(rand.nextInt(tieredCards.size()));
                    if(!toReward.getOwnedCards().contains(randomCard)) {
                        return new CardReward(randomCard, null) {
                            @Override
                            public void reward(Player p) {
                                if(toReward.getOwnedCards().contains(randomCard)) {
                                    showTo.playSound(showTo.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7F, 0.6F);
                                    showTo.sendMessage("§5You rolled a duplicate card! §bHere's " + wisdomEpicAmt + " wisdom instead.");
                                    return;
                                }
                                super.reward(p);
                            }
                        };
                    }
                }
                return new WisdomReward(wisdomEpicAmt);
        }
        int wisdomAmt = (int) (chance * 200) + 50;
        return new WisdomReward(wisdomAmt);
    }


    @Nullable
    public static CardPack getPack(int id) {
        if(id < 0 || CardPack.values().length <= id)
            return null;

        return CardPack.values()[id];
    }
}
