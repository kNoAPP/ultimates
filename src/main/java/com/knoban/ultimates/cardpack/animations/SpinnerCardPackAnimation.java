package com.knoban.ultimates.cardpack.animations;

import com.knoban.atlas.gui.GUI;
import com.knoban.atlas.utils.SoundBundle;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.aspects.Items;
import com.knoban.ultimates.cardholder.CardHolder;
import com.knoban.ultimates.cardpack.CardPack;
import com.knoban.ultimates.rewards.Reward;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Random;

public class SpinnerCardPackAnimation extends CardPackAnimation {

    protected final GUI spinner;

    protected int animationTicks;

    protected static final Random rand = new Random();

    public SpinnerCardPackAnimation(Ultimates plugin, Player awardTo, CardPack opening) {
        this(plugin, awardTo, opening, true);
    }

    public SpinnerCardPackAnimation(Ultimates plugin, Player awardTo, CardPack opening, boolean award) {
        super(plugin, awardTo, opening, award);
        this.rewards = new Reward[]{opening.getRandomReward(rewardTo)};
        this.animationTicks = 0;

        spinner = new GUI(plugin, opening.getName() + " Card Pack", 27,
                null, null,
                new SoundBundle(Sound.BLOCK_NOTE_BLOCK_BASS, 1F, 1F),
                new SoundBundle(Sound.BLOCK_NOTE_BLOCK_BASS, 1F, 1F));

        // Place placeholders
        for(int i=0; i<9; i++)
            spinner.setSlot(i, Items.GUI_PLACEHOLDER_LIGHT_GRAY_ITEM);
        spinner.setSlot(4, Items.CARDPACK_SPINNER_ITEM);
        for(int i=18; i<27; i++)
            spinner.setSlot(i, Items.GUI_PLACEHOLDER_LIGHT_GRAY_ITEM);

        // Reward fulfillment
        spinner.setOnCloseCallback(() -> {
            if(willAward) {
                CardHolder cardHolder = CardHolder.getCardHolder(awardTo);
                for(Reward reward : rewards)
                    reward.reward(cardHolder);
            }
        });
    }

    protected static final int MAX_PHASE = 3;
    @Override
    public void play() {
        spinner.openInv(rewardTo.getPlayer());
        animationLoop(MAX_PHASE, getNextMaxPhase(MAX_PHASE));
    }

    protected void animationLoop(int phase, int cycles) {
        if(spinner.isDestroyed())
            return;

        Reward randomReward = opening.getRandomReward(rewardTo);
        if(phase == 0 && cycles == 4)
            this.rewards = new Reward[]{randomReward};
        spinner.shiftRow(1, -1);
        spinner.shiftRow(2, -1);
        spinner.setSlot(17, randomReward.getIcon());
        spinner.setSlot(26, randomReward.getTier().getPlaceholder());

        playAnimationTickSound();

        if(cycles == 0) {
            if(phase == 0) {
                playAnimationTickSoundRift();
                awardTo.playSound(awardTo.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1F, 1F);
                awardTo.playSound(awardTo.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1F, 1F);
                // Place placeholders
                for(int i=0; i<9; i++)
                    spinner.setSlot(i, Items.GUI_PLACEHOLDER_LIME_ITEM);
                spinner.setSlot(4, Items.CARDPACK_SPINNER_ITEM);

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

    protected long getDelay(int phase) {
        switch(phase) {
            case 3:
                return 2L;
            case 2:
                return 3L;
            case 1:
                return 5L;
            case 0:
                return 12L;
            default:
                return 1L;
        }
    }

    protected int getNextMaxPhase(int phase) {
        switch(phase) {
            case 3:
                return rand.nextInt(20) + 30;
            case 2:
                return rand.nextInt(10) + 15;
            case 1:
                return rand.nextInt(4) + 12;
            case 0:
                return 5 + rand.nextInt(2); // No less than 5, or things will break here.
            default:
                return rand.nextInt(50) + 50;
        }
    }

    protected void playAnimationTickSound() {
        int scale = (animationTicks / 3) % 5;
        int note;
        switch(animationTicks % 3) {
            case 0:
                note = 0;
                break;
            case 1:
                note = 4;
                break;
            default:
                note = 7;
                break;
        }

        awardTo.playSound(awardTo.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1F, (float) Math.pow(2, (note + scale - 12.0)/12.0));
    }

    protected void playAnimationTickSoundRift() {
        int scale = (animationTicks / 3) % 5;
        awardTo.playSound(awardTo.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1F, (float) Math.pow(2, (scale - 12.0)/12.0));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> awardTo.playSound(awardTo.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1F, (float) Math.pow(2, (4 + scale - 12.0)/12.0)), 1L);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> awardTo.playSound(awardTo.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1F, (float) Math.pow(2, (7 + scale - 12.0)/12.0)), 2L);
    }
}
