package com.knoban.ultimates.battlepass;

import com.knoban.ultimates.rewards.Reward;

import java.util.Map;

public class BattlePassLevel {

    private final int level;
    private final Reward free, premium;

    public BattlePassLevel(int level, Map<String, Object> data) throws Exception {
        this.level = level;
        Map<String, Object> freeData = (Map<String, Object>) data.get("free");
        this.free = freeData != null ? Reward.fromData(freeData) : null;
        Map<String, Object> premiumData = (Map<String, Object>) data.get("premium");
        this.premium = premiumData != null ? Reward.fromData(premiumData) : null;
    }

    public int getLevel() {
        return level;
    }

    public Reward getFree() {
        return free;
    }

    public Reward getPremium() {
        return premium;
    }
}
