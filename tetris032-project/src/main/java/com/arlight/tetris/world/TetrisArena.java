package com.arlight.tetris.world;

import net.minecraft.core.BlockPos;
import java.util.ArrayList;
import java.util.List;

public final class TetrisArena {
    public static final int PLATFORM_RADIUS = 14;
    public static final int POD_RING_RADIUS = 10;
    public static final int POD_SIZE = 3;
    private TetrisArena() {}

    public static List<BlockPos> podCenters(BlockPos center, int count) {
        List<BlockPos> pods = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            int x = center.getX() + (int) Math.round(Math.cos(angle) * POD_RING_RADIUS);
            int z = center.getZ() + (int) Math.round(Math.sin(angle) * POD_RING_RADIUS);
            pods.add(new BlockPos(x, center.getY(), z));
        }
        return pods;
    }

    public static float podYawFacingCenter(BlockPos center, BlockPos pod) {
        double dx = center.getX() - pod.getX();
        double dz = center.getZ() - pod.getZ();
        return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
    }

    public static BlockPos standingPos(BlockPos pod) { return pod.above(); }
}
