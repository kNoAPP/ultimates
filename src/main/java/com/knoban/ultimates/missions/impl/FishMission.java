package com.knoban.ultimates.missions.impl;

import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.missions.Mission;
import com.knoban.ultimates.missions.MissionInfo;
import com.knoban.ultimates.missions.bossbar.BossBarConstant;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerFishEvent;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Map;

@MissionInfo(name = "fish")
public class FishMission extends Mission {

    private static EnumSet<Material> FISH = EnumSet.of(
            Material.TROPICAL_FISH, Material.PUFFERFISH, Material.COD, Material.SALMON
    );

    public FishMission(@NotNull Ultimates plugin, @NotNull String uuid, @NotNull Map<String, Object> missionData) {
        super(plugin, uuid, missionData);
        this.display = "§bCatch " + maxProgress + " Fish";
        this.material = Material.COD;
        this.description = new String[]{"§9Use a fishing pole and", "§9catch some fish."};

        bossBarInformation.setTitle(display + " §7(§c" + BossBarConstant.PROGRESS_LEFT + " left§7)");
        bossBarInformation.setStyle(BarStyle.SOLID);
        bossBarInformation.setColor(BarColor.BLUE);
        bossBarInformation.setFlags(); // No flags
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTrade(PlayerFishEvent e) {
        if(e.getState() == PlayerFishEvent.State.CAUGHT_FISH && e.getCaught() != null) {
            Item item = (Item) e.getCaught();
            if(FISH.contains(item.getItemStack().getType())) {
                Player p = e.getPlayer();
                incrementProgress(p, 1);
            }
        }
    }
}
