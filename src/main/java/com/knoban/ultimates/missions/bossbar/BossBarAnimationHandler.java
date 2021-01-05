package com.knoban.ultimates.missions.bossbar;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.knoban.ultimates.Ultimates;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * This class performs a fluid boss bar animation between multiple or single calls to the animation. For example,
 * a single request to perform an animation with do just that: perform the animation. Multiple calls will not restart
 * an ongoing animation, but it will dynamically adapt them with the new passed values.
 * <br><br>
 * The intent of this class is to provide fluid, good looking UI mainly for the use of {@link com.knoban.ultimates.missions.Mission}s.
 *
 * @author Alden Bansemer (kNoAPP)
 */
public class BossBarAnimationHandler {

    private final Ultimates plugin;

    private final ConcurrentHashMap<String, BossBarInformation> information = new ConcurrentHashMap<>();
    private final Cache<Player, TreeMap<String, BossBarAnimation>> animators = CacheBuilder.newBuilder()
            .expireAfterAccess(5L, TimeUnit.SECONDS).build();

    /**
     * Creates an instance of {@link BossBarAnimationHandler}.
     * @param plugin An instance of the plugin.
     */
    public BossBarAnimationHandler(Ultimates plugin) {
        this.plugin = plugin;
    }

    /**
     * Plays a {@link BossBar} animation to the passed {@link Player} using the {@link BossBarInformation} stored to
     * the passed {@link String}. To store or overwrite an existing {@link BossBarInformation},
     * see {@link #setSettings(String, BossBarInformation)}
     * @param player The {@link Player} to play the animation for.
     * @param uuid The {@link} UUID containing the {@link BossBarInformation}. If invalid, a default bar will be used.
     * @param oldValue The old value of the boss bar.
     * @param newValue The new value of the boss bar.
     * @param maxValue The maximum value of the boss bar.
     */
    public void playAnimation(Player player, String uuid, long oldValue, long newValue, long maxValue) {
        TreeMap<String, BossBarAnimation> map = animators.getIfPresent(player);
        if(map == null) {
            map = new TreeMap<>();
            animators.put(player, map);
        }

        BossBarAnimation animation = map.get(uuid);
        if(animation == null) {
            animation = new BossBarAnimation(player, information.getOrDefault(uuid, BossBarConstant.getDefaultBossBar()));
            map.put(uuid, animation);
        }

        animation.animate(plugin, oldValue, newValue, maxValue);
    }

    /**
     * Stores some {@link BossBarInformation} linked to the passed key.
     * @param uuid The {@link String} key
     * @param info The {@link BossBarInformation} to link to.
     */
    public void setSettings(String uuid, BossBarInformation info) {
        information.put(uuid, info);
    }

    /**
     * Sets a key/value pair of the {@link BossBarInformation} back to defaults.
     * @param uuid The {@link String} to reset to defaults.
     */
    public void removeSettings(String uuid) {
        information.remove(uuid);
    }

    /**
     * A helper class to perform the boss bar animation.
     * @author Alden Bansemer (kNoAPP)
     */
    private static class BossBarAnimation {

        private Player player;
        private String titleTemplate;
        private BossBar bar;

        private byte phase;
        private long ticks;
        private BukkitTask task;

        private long oldValue;
        private double oldProgress, newProgress;

        /**
         * Create a {@link Player} specific animation.
         */
        private BossBarAnimation(Player player, BossBarInformation info) {
            this.player = player;
            this.titleTemplate = info.getTitle();
            this.bar = Bukkit.createBossBar(titleTemplate, info.getColor(), info.getStyle(), info.getFlags());
        }

        /**
         * 0. Do nothing. Wait for animate to be called.
         * 1. Show boss bar at oldValue for 1/2s.
         * 2. Curve animation of value to newValue over 2s.
         * 3. Show boss bar newValue for 2s.
         *
         * If further progress is made during the animation.
         * 0. During part (0), do part 1.
         * 1. During part (1), continue as normal with the newValue. Update title.
         * 2. During part (2), adjust curve for newValue so the newValue is met smoothly
         * 3. During part (3), cancel part 3 early, perform part 1 instantly, go right into part 2.
         */
        private static final long P1_TICKS = 5L;
        private static final long P2_TICKS = 40L;
        private static final long P3_TICKS = 40L;

        /**
         * Performs the animation between multiple calls.
         */
        private void animate(Ultimates plugin, long oldValue, long newValue, long maxValue) {
            if(newValue > maxValue)
                newValue = maxValue;

            double oldProgress = (double) oldValue / maxValue;
            if(oldProgress > 1)
                oldProgress = 1;
            else if(oldProgress < 0)
                oldProgress = 0;
            double newProgress = (double) newValue / maxValue;
            if(newProgress > 1)
                newProgress = 1;
            else if(newProgress < 0)
                newProgress = 0;
            this.newProgress = newProgress;

            switch(phase) {
                case 0:
                    phase = 1;
                    this.oldValue = oldValue;
                    this.oldProgress = oldProgress;

                    bar.addPlayer(player);
                    bar.setProgress(oldProgress);
                    task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        phase = 2;
                        ticks = 0;
                        task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                            double prctCompleteAnimation = (double) ticks / P2_TICKS;
                            double animationOffset = easeInOutCubic(prctCompleteAnimation);
                            double animation = animationOffset*(this.newProgress - this.oldProgress) + this.oldProgress;
                            bar.setProgress(animation);

                            if(ticks++ >= P2_TICKS) {
                                task.cancel();
                                phase = 3;
                                task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                    phase = 0;
                                    bar.removePlayer(player);
                                    task = null;
                                }, P3_TICKS);
                            }
                        }, 1L, 1L);
                    }, 10L);
                    break;
                case 3:
                    phase = 2;
                    ticks = 0;
                    this.oldValue = oldValue;
                    this.oldProgress = oldProgress;
                    task.cancel();

                    bar.setProgress(oldProgress);

                    task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                        double prctCompleteAnimation = (double) ticks / P2_TICKS;
                        double animationOffset = easeInOutCubic(prctCompleteAnimation);
                        double animation = animationOffset*(this.newProgress - this.oldProgress) + this.oldProgress;
                        bar.setProgress(animation);

                        if(ticks++ >= P2_TICKS) {
                            task.cancel();
                            phase = 3;
                            task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                phase = 0;
                                bar.removePlayer(player);
                                task = null;
                            }, P3_TICKS);
                        }
                    }, 1L, 1L);
                    break;
                default:
                    break;
            }

            String title = applyData(this.oldValue, newValue, maxValue, this.oldProgress, newProgress);
            bar.setTitle(title);
        }

        /**
         * Helper method to replace {@link BossBarConstant}s with their rightful values.
         */
        private String applyData(long oldValue, long newValue, long maxValue, double oldProgress, double newProgress) {
            return titleTemplate.replaceAll(BossBarConstant.OLD_PROGRESS, String.valueOf(oldValue))
                    .replaceAll(BossBarConstant.NEW_PROGRESS, String.valueOf(newValue))
                    .replaceAll(BossBarConstant.MAX_PROGRESS, String.valueOf(maxValue))
                    .replaceAll(BossBarConstant.OLD_RATIO, String.format("%.2f", oldProgress*100) + '%')
                    .replaceAll(BossBarConstant.NEW_RATIO, String.format("%.2f", newProgress*100) + '%')
                    .replaceAll(BossBarConstant.PROGRESS_LEFT, String.valueOf(maxValue - newValue));
        }

        /**
         * See https://easings.net/#easeInOutCubic
         */
        private double easeInOutCubic(double prctComplete) {
            return prctComplete < 0.5 ? 4 * prctComplete * prctComplete * prctComplete : 1 - Math.pow(-2 * prctComplete + 2, 3) / 2;
        }
    }

    /**
     * A simple class that holds information about the style of {@link BossBar} to show during animation.
     *
     * @author Alden Bansemer
     */
    public static class BossBarInformation {

        private String title;
        private BarStyle style;
        private BarColor color;
        private BarFlag[] flags;

        public BossBarInformation() {}

        public BossBarInformation(String title, BarStyle style, BarColor color, BarFlag... flags) {
            this.title = title;
            this.style = style;
            this.color = color;
            this.flags = flags;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public BarStyle getStyle() {
            return style;
        }

        public void setStyle(BarStyle style) {
            this.style = style;
        }

        public BarColor getColor() {
            return color;
        }

        public void setColor(BarColor color) {
            this.color = color;
        }

        public BarFlag[] getFlags() {
            return flags;
        }

        public void setFlags(BarFlag... flags) {
            this.flags = flags;
        }
    }
}
