package com.knoban.ultimates.missions;

import com.knoban.ultimates.missions.impl.*;

import java.util.*;

public final class Missions {

    private static final List<Class<? extends Mission>> missions = Collections.unmodifiableList(Arrays.asList(
            BlockBreakMission.class, BlockPlaceMission.class, KillEntityMission.class, FishMission.class,
            DestinationMission.class
    ));

    private final Map<String, Class<? extends Mission>> missionByName;

    private static final Missions INSTANCE = new Missions();

    private Missions() {
        HashMap<String, Class<? extends Mission>> missionByName = new HashMap<>();
        for(Class<? extends Mission> mission : missions)
            missionByName.put(mission.getAnnotation(MissionInfo.class).name(), mission);
        this.missionByName = Collections.unmodifiableMap(missionByName);
    }

    public List<Class<? extends Mission>> getMissions() {
        return missions;
    }

    public Map<String, Class<? extends Mission>> getMissionByName() {
        return missionByName;
    }

    public static Missions getInstance() {
        return INSTANCE;
    }
}
