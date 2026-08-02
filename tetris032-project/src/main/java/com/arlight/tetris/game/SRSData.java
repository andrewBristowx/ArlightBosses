package com.arlight.tetris.game;

public final class SRSData {
    private SRSData() {}

    public static final int[][][] JLSTZ_KICKS = {
            {{0,0}, {-1,0}, {-1,1}, {0,-2}, {-1,-2}},
            {{0,0}, {1,0}, {1,-1}, {0,2}, {1,2}},
            {{0,0}, {1,0}, {1,-1}, {0,2}, {1,2}},
            {{0,0}, {-1,0}, {-1,1}, {0,-2}, {-1,-2}},
            {{0,0}, {1,0}, {1,1}, {0,-2}, {1,-2}},
            {{0,0}, {-1,0}, {-1,-1}, {0,2}, {-1,2}},
            {{0,0}, {-1,0}, {-1,-1}, {0,2}, {-1,2}},
            {{0,0}, {1,0}, {1,1}, {0,-2}, {1,-2}}
    };

    public static final int[][][] I_KICKS = {
            {{0,0}, {-2,0}, {1,0}, {-2,-1}, {1,2}},
            {{0,0}, {2,0}, {-1,0}, {2,1}, {-1,-2}},
            {{0,0}, {-1,0}, {2,0}, {-1,2}, {2,-1}},
            {{0,0}, {1,0}, {-2,0}, {1,-2}, {-2,1}},
            {{0,0}, {2,0}, {-1,0}, {2,1}, {-1,-2}},
            {{0,0}, {-2,0}, {1,0}, {-2,-1}, {1,2}},
            {{0,0}, {1,0}, {-2,0}, {1,-2}, {-2,1}},
            {{0,0}, {-1,0}, {2,0}, {-1,2}, {2,-1}}
    };

    public static int transitionIndex(int from, int to) {
        if (from == 0 && to == 1) return 0;
        if (from == 1 && to == 0) return 1;
        if (from == 1 && to == 2) return 2;
        if (from == 2 && to == 1) return 3;
        if (from == 2 && to == 3) return 4;
        if (from == 3 && to == 2) return 5;
        if (from == 3 && to == 0) return 6;
        if (from == 0 && to == 3) return 7;
        throw new IllegalArgumentException("Transición inválida: " + from + "->" + to);
    }

    public static int[][] getKicks(TetrominoType type, int from, int to) {
        int idx = transitionIndex(from, to);
        if (type == TetrominoType.O) return new int[][]{{0, 0}};
        return type == TetrominoType.I ? I_KICKS[idx] : JLSTZ_KICKS[idx];
    }
}
