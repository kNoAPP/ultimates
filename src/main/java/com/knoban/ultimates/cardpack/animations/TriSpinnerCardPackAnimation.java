package com.knoban.ultimates.cardpack.animations;

import com.knoban.atlas.rewards.Reward;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.aspects.Items;
import com.knoban.ultimates.cardpack.CardPack;
import com.knoban.ultimates.primal.Tier;
import com.knoban.ultimates.rewards.CardReward;
import com.knoban.ultimates.rewards.WisdomReward;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class TriSpinnerCardPackAnimation extends SpinnerCardPackAnimation {

    public TriSpinnerCardPackAnimation(Ultimates plugin, Player awardTo, CardPack opening) {
        this(plugin, awardTo, opening, true);
    }

    public TriSpinnerCardPackAnimation(Ultimates plugin, Player awardTo, CardPack opening, boolean award) {
        super(plugin, awardTo, opening, award);
        this.rewards = new Reward[]{opening.getRandomReward(rewardTo),
                opening.getRandomReward(rewardTo),
                opening.getRandomReward(rewardTo)};
        spinner.setSlot(3, Items.CARDPACK_SPINNER_ITEM);
        spinner.setSlot(5, Items.CARDPACK_SPINNER_ITEM);
    }

    @Override
    protected void animationLoop(int phase, int cycles) {
        if(spinner.isDestroyed())
            return;

        Reward randomReward = opening.getRandomReward(rewardTo);
        if(phase == 0 && (3 <= cycles && cycles <= 5)) {
            this.rewards[5 - cycles] = randomReward;
        }
        spinner.shiftRow(1, -1);
        spinner.shiftRow(2, -1);
        spinner.setSlot(17, randomReward.getIcon());
        if(randomReward instanceof CardReward) {
            CardReward cardReward = (CardReward) randomReward;
            spinner.setSlot(26, cardReward.getCard().getInfo().tier().getPlaceholder());
        } else if(randomReward instanceof WisdomReward) {
            WisdomReward wisdomReward = (WisdomReward) randomReward;
            if(wisdomReward.getAmount() < 200)
                spinner.setSlot(26, Tier.COMMON.getPlaceholder());
            else if(wisdomReward.getAmount() < 500)
                spinner.setSlot(26, Tier.RARE.getPlaceholder());
            else if(wisdomReward.getAmount() < 2000)
                spinner.setSlot(26, Tier.EPIC.getPlaceholder());
            else
                spinner.setSlot(26, Tier.LEGENDARY.getPlaceholder());
        }

        playAnimationTickSound();

        if(cycles == 0) {
            if(phase == 0) {
                playAnimationTickSoundRift();
                awardTo.playSound(awardTo.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1F, 1F);
                awardTo.playSound(awardTo.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1F, 1F);
                // Place placeholders
                for(int i=0; i<9; i++)
                    spinner.setSlot(i, Items.GUI_PLACEHOLDER_LIME_ITEM);
                spinner.setSlot(3, Items.CARDPACK_SPINNER_ITEM);
                spinner.setSlot(4, Items.CARDPACK_SPINNER_ITEM);
                spinner.setSlot(5, Items.CARDPACK_SPINNER_ITEM);

            } else {
                plugin.getServer().getScheduler().runTaskLater(plugin,
                        () -> animationLoop(phase - 1, getNextMaxPhase(phase - 1)),
                        getDelay(phase - 1));
            }
        } else
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> animationLoop(phase, cycles - 1), getDelay(phase));

        animationTicks++;
    }
}
