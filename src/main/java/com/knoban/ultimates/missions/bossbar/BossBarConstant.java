package com.knoban.ultimates.missions.bossbar;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;

/**
 * Provides constants that can be used for {@link BossBarAnimationHandler}.
 *
 * @author Alden Bansemer (kNoAPP)
 */
public class BossBarConstant {

    public static final String OLD_PROGRESS = "&op",
            NEW_PROGRESS = "&np",
            MAX_PROGRESS = "&mp",
            OLD_RATIO = "&or",
            NEW_RATIO = "&nr",
            PROGRESS_LEFT = "&pl";

    public static BossBarAnimationHandler.BossBarInformation getDefaultBossBar() {
        BossBarAnimationHandler.BossBarInformation information = new BossBarAnimationHandler.BossBarInformation();
        information.setTitle("§7Undefined");
        information.setStyle(BarStyle.SOLID);
        information.setColor(BarColor.WHITE);
        information.setFlags(BarFlag.CREATE_FOG);
        return information;
    }
}
