package com.knoban.ultimates.missions;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.knoban.atlas.gui.GUI;
import com.knoban.atlas.gui.GUIClickable;
import com.knoban.atlas.scheduler.ClockedTask;
import com.knoban.atlas.scheduler.ClockedTaskManager;
import com.knoban.atlas.utils.SoundBundle;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.aspects.Items;
import com.knoban.ultimates.cardholder.CardHolder;
import com.knoban.ultimates.rewards.Reward;
import com.knoban.ultimates.rewards.impl.CardReward;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MissionManager implements Listener {

    private final Ultimates plugin;

    private final DatabaseReference missionsTimelineReference;
    private final ChildEventListener listener;

    private final ConcurrentHashMap<String, Mission> allMissions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Mission> activeMissions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ClockedTask> missionTasks = new ConcurrentHashMap<>();

    public MissionManager(Ultimates plugin) {
        this.plugin = plugin;

        // Storage of mission timeline with info relating to mission type, uuid, max progress, expirations, etc.
        this.missionsTimelineReference = plugin.getFirebase().getDatabase().getReference("/missions/timeline");

        missionsTimelineReference.addChildEventListener(listener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                // Mission added to timeline. We need to prepare it to go live in memory.
                // Called once initially and again each time an event fires.
                addMission(snapshot.getKey(), (Map<String, Object>) snapshot.getValue());
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                // Mission modified. Likely means the expiration time or max progress changed.
                // Usually we never want to do this. We'd just make a new mission.
                // But we will support it anyway for the dumbass who wants to change the mission mid-way through
                // and upset players.
                removeMission(snapshot.getKey());
                addMission(snapshot.getKey(), (Map<String, Object>) snapshot.getValue());
            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {
                // Mission removed from timeline. We need to remove it from memory.
                removeMission(snapshot.getKey());
            }

            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {
                // Won't happen. Doesn't make sense in this context. Don't implement.
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Database error. Print it.
                plugin.getLogger().warning("Mission listener error: " + error.getMessage());
            }
        });
    }

    private void addMission(String missionId, Map<String, Object> missionData) {
        plugin.getLogger().info("Mission loading: " + missionId);
        String type = (String) missionData.get("type");
        if(type == null) {
            plugin.getLogger().warning("Missing mission type on: " + missionId);
            plugin.getLogger().warning("This was likely caused by an incorrect manual entry to Firebase. You can safely ignore this warning.");
            return;
        }

        Class<? extends Mission> missionClass = Missions.getInstance().getMissionByName().get(type);
        if(missionClass == null) {
            plugin.getLogger().warning("Failed to parse mission class: " + type);
            plugin.getLogger().warning("This was likely caused by an incorrect manual entry to Firebase. You can safely ignore this warning.");
            return;
        }

        Mission instance;
        try {
            instance = missionClass
                    .getConstructor(Ultimates.class, String.class, Map.class)
                    .newInstance(plugin, missionId, missionData);
        } catch(Exception e) {
            plugin.getLogger().warning("Failed to create mission (" + missionId + "): " + type);
            e.printStackTrace();
            return;
        }

        allMissions.put(missionId, instance);

        long currentTime = System.currentTimeMillis();
        if(currentTime < instance.getStartTime()) {
            plugin.getLogger().info("Mission enqueued: " + instance);

            // Plan to start the mission!
            ClockedTask task = new ClockedTask(instance.getStartTime(), true, () -> {
                missionTasks.remove(missionId);
                startActiveMission(missionId, instance);
            });
            missionTasks.put(missionId, task);
            ClockedTaskManager.getManager().addTask(task);
        } else if(instance.getStartTime() <= currentTime && currentTime < instance.getEndTime()) {
            // Mission is active! Plan to end the mission.
            startActiveMission(missionId, instance);
        } else
            plugin.getLogger().info("Mission already finished: " + instance);// Otherwise, nothing more is needed if the mission is already ended.
    }

    /**
     * Helper method
     */
    private void startActiveMission(String missionId, Mission instance) {
        activeMissions.put(missionId, instance);
        instance.setActive(true);
        plugin.getLogger().info("Mission started: " + instance);

        ClockedTask task = new ClockedTask(instance.getEndTime(), true, () -> {
            activeMissions.remove(missionId);
            instance.setActive(false);
            missionTasks.remove(missionId);

            plugin.getLogger().info("Mission ended: " + instance);
        });
        missionTasks.put(missionId, task);
        ClockedTaskManager.getManager().addTask(task);
    }

    private void removeMission(String missionId) {
        plugin.getLogger().info("Mission unloading: " + missionId);

        allMissions.remove(missionId);
        Mission mission = activeMissions.remove(missionId);
        if(mission != null)
            mission.setActive(false);
        ClockedTask task = missionTasks.remove(missionId);
        if(task != null)
            task.setCancelled(true);
    }

    public void registerDataListener(UUID uuid) {
        for(Mission active : activeMissions.values())
            active.registerPlayerDataListener(uuid);
    }

    public void unregisterDataListener(UUID uuid) {
        for(Mission active : activeMissions.values())
            active.unregisterPlayerDataListener(uuid);
    }

    public Map<String, Mission> getMissions() {
        return Collections.unmodifiableMap(allMissions);
    }

    public Map<String, Mission> getActiveMissions() {
        return Collections.unmodifiableMap(activeMissions);
    }

    public void openMissionGUI(Player player) {
        openMissionGUI(player, CardHolder.getCardHolder(player), true);
    }

    public void openMissionGUI(Player showTo, CardHolder player, boolean withOpenSounds) {
        if(!player.isLoaded()) {
            showTo.sendMessage(CardHolder.UNLOADED_MESSAGE);
            return;
        }

        int activeMissionCount = activeMissions.values().size();
        final boolean expandGUI = activeMissionCount > 9;

        GUI gui = withOpenSounds ? new GUI(plugin, "Missions", expandGUI ? 54 : 27,
                new SoundBundle(Sound.BLOCK_CHEST_OPEN, 1F, 0.9F),
                null,
                new SoundBundle(Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F),
                new SoundBundle(Sound.ENTITY_LINGERING_POTION_THROW, 1F, 1.5F))
                : new GUI(plugin, "Missions", expandGUI ? 54 : 27,
                null,
                null,
                new SoundBundle(Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F),
                new SoundBundle(Sound.ENTITY_LINGERING_POTION_THROW, 1F, 1.5F));

        // Back Button
        GUIClickable back = new GUIClickable();
        back.setActionOnClick((g, e) -> {
            player.openBattlePassMainGUI(showTo, false);
            showTo.playSound(showTo.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1F, 1F);
        });
        gui.setSlot(0, Items.BACK_ITEM, back);

        // Explanation Item
        gui.setSlot(4, Items.MISSIONS_EXPLANATION_ITEM);

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            PriorityQueue<Mission> queue = new PriorityQueue<>(activeMissions.values());
            boolean currentExpandGUI = queue.size() > 9;
            if(currentExpandGUI == expandGUI)
                updateMissionGUI(player, gui, queue);
            else { // On inventory resize...
                showTo.closeInventory();
                openMissionGUI(showTo, player, false);
            }
        }, 0L, 20L);
        gui.setOnDestroyCallback(task::cancel);

        gui.openInv(showTo);
    }

    private void updateMissionGUI(CardHolder player, GUI gui, Queue<Mission> missions) {
        if(!player.isLoaded())
            return;

        UUID uuid = player.getUniqueId();
        ItemStack air = new ItemStack(Material.AIR);
        for(int i=9; i<27; i++)
            gui.setSlot(i, air);

        MissionLayout layout = MissionLayout.of(missions.size());
        for(int i : layout.getLayout()) {
            Mission mission = missions.poll();
            assert mission != null;
            gui.setSlot(9+i, mission.getMissionItem(uuid));
            Reward reward = mission.getReward();

            if(reward == null)
                continue;

            /*if(reward instanceof SpecificReward)
                gui.setSlot(18+i, ((SpecificReward) reward).getIcon(player));
            else*/
            gui.setSlot(18+i, mission.getReward().getIcon());
        }

        if(missions.isEmpty())
            return;

        for(int i=36; i<54; i++)
            gui.setSlot(i, air);

        layout = MissionLayout.of(missions.size());
        for(int i : layout.getLayout()) {
            Mission mission = missions.poll();
            gui.setSlot(36+i, mission.getMissionItem(uuid));
            Reward reward = mission.getReward();

            if(reward == null)
                continue;

            if(reward instanceof CardReward) // Special case until I can find a better way to do this.
                gui.setSlot(45+i, ((CardReward) reward).getIcon(player));
            else
                gui.setSlot(45+i, mission.getReward().getIcon());
        }
    }

    public void safeShutdown() {
        missionsTimelineReference.removeEventListener(listener);
        for(Mission mission : new ArrayList<>(allMissions.values()))
            removeMission(mission.getUuid());
        HandlerList.unregisterAll(this);
    }
}
