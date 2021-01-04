package com.knoban.ultimates.rewards;

import com.knoban.ultimates.rewards.impl.*;

import java.util.*;

public final class Rewards {

    private static final List<Class<? extends Reward>> rewards = Collections.unmodifiableList(Arrays.asList(
            CardReward.class, WisdomReward.class, ExperienceReward.class, EstateClaimReward.class, CardSlotReward.class,
            ItemStackReward.class
    ));

    private static final Rewards INSTANCE = new Rewards();

    private final Map<String, Class<? extends Reward>> rewardByName;

    private Rewards() {
        HashMap<String, Class<? extends Reward>> rewardByName = new HashMap<>();
        for(Class<? extends Reward> reward : rewards)
            rewardByName.put(reward.getAnnotation(RewardInfo.class).name(), reward);
        this.rewardByName = Collections.unmodifiableMap(rewardByName);
    }

    public List<Class<? extends Reward>> getRewards() {
        return rewards;
    }

    public Map<String, Class<? extends Reward>> getRewardByName() {
        return rewardByName;
    }

    public static Rewards getInstance() {
        return INSTANCE;
    }
}
