package com.knoban.ultimates.missions;

public enum MissionLayout {

    ZERO(),
    ONE(4),
    TWO(2, 6),
    THREE(2, 4, 6),
    FOUR(1, 3, 5, 7),
    FIVE(0, 2, 4, 6, 8),
    SIX(1, 2, 3, 5, 6, 7),
    SEVEN(1, 2, 3, 4, 5, 6, 7),
    EIGHT(0, 1, 2, 3, 5, 6, 7, 8),
    NINE(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

    private int[] layout;

    MissionLayout(int... layout) {
        this.layout = layout;
    }

    private int getNumberOfMissions() {
        return ordinal();
    }

    public int[] getLayout() {
        return layout;
    }

    private int indexOf(int index) {
        if(index < 0 || layout.length <= index)
            return -1;

        return layout[index];
    }

    public static MissionLayout of(int items) {
        if(items < 0)
            return values()[0];
        if(items > 9)
            return values()[9];


        return values()[items];
    }
}
