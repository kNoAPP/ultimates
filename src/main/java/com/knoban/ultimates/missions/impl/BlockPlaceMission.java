package com.knoban.ultimates.missions.impl;

import com.knoban.atlas.utils.Tools;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.missions.Mission;
import com.knoban.ultimates.missions.MissionInfo;
import com.knoban.ultimates.missions.bossbar.BossBarConstant;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@MissionInfo(name = "block-place")
public class BlockPlaceMission extends Mission {

    public BlockPlaceMission(@NotNull Ultimates plugin, @NotNull String uuid, @NotNull Map<String, Object> missionData) {
        super(plugin, uuid, missionData);
        String materialName = (String) extraData.getOrDefault("material", "DIAMOND_ORE");
        this.material = Material.getMaterial(materialName);
        if(material == null)
            throw new IllegalArgumentException("Invalid material: " + materialName);
        this.display = "§7Place §b" + maxProgress + " " + Tools.enumNameToHumanReadable(material.name());
        this.description = new String[]{"§aGather and place " + "§a" + Tools.enumNameToHumanReadable(material.name())};

        bossBarInformation.setTitle(display + " §7(§e" + BossBarConstant.PROGRESS_LEFT + " left§7)");
        bossBarInformation.setStyle(BarStyle.SEGMENTED_6);
        bossBarInformation.setColor(BarColor.RED);
        bossBarInformation.setFlags(); // No flags
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if(e.getBlock().getType().equals(material))
            incrementProgress(p, 1L);
    }
}
