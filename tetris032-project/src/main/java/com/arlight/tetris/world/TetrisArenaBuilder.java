package com.arlight.tetris.world;

import com.arlight.tetris.multiplayer.MatchConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class TetrisArenaBuilder {
    private static final BlockState PLATFORM = Blocks.BLACK_CONCRETE.defaultBlockState();
    private static final BlockState RING = Blocks.SEA_LANTERN.defaultBlockState();
    private static final BlockState POD_WALL = Blocks.GRAY_STAINED_GLASS.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState[] POD_COLORS = {
            Blocks.RED_CONCRETE.defaultBlockState(), Blocks.ORANGE_CONCRETE.defaultBlockState(),
            Blocks.YELLOW_CONCRETE.defaultBlockState(), Blocks.LIME_CONCRETE.defaultBlockState(),
            Blocks.CYAN_CONCRETE.defaultBlockState(), Blocks.BLUE_CONCRETE.defaultBlockState(),
            Blocks.PURPLE_CONCRETE.defaultBlockState(), Blocks.MAGENTA_CONCRETE.defaultBlockState()
    };
    private TetrisArenaBuilder() {}

    public static void build(ServerLevel level, BlockPos center) {
        clear(level, center);
        buildPlatform(level, center);
        buildPods(level, center);
    }

    private static void clear(ServerLevel level, BlockPos center) {
        int r = TetrisArena.PLATFORM_RADIUS + 2;
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
            if (x * x + z * z > r * r) continue;
            for (int y = -1; y <= 4; y++) level.setBlock(center.offset(x, y, z), AIR, 2);
        }
    }

    private static void buildPlatform(ServerLevel level, BlockPos center) {
        int r = TetrisArena.PLATFORM_RADIUS;
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
            double dist = Math.sqrt(x * x + z * z);
            if (dist <= r) level.setBlock(center.offset(x, 0, z), dist > r - 1 ? RING : PLATFORM, 2);
        }
    }

    private static void buildPods(ServerLevel level, BlockPos center) {
        List<BlockPos> pods = TetrisArena.podCenters(center, MatchConfig.MAX_PLAYERS);
        int half = TetrisArena.POD_SIZE / 2;
        for (int i = 0; i < pods.size(); i++) {
            BlockPos pod = pods.get(i);
            BlockState color = POD_COLORS[i % POD_COLORS.length];
            for (int dx = -half; dx <= half; dx++) for (int dz = -half; dz <= half; dz++)
                level.setBlock(pod.offset(dx, 0, dz), color, 2);
            for (int dx = -half; dx <= half; dx++) for (int dz = -half; dz <= half; dz++) {
                boolean edge = Math.abs(dx) == half || Math.abs(dz) == half;
                boolean corner = Math.abs(dx) == half && Math.abs(dz) == half;
                if (!edge || corner || facesCenterSide(dx, dz, center, pod)) continue;
                for (int dy = 1; dy <= 2; dy++) level.setBlock(pod.offset(dx, dy, dz), POD_WALL, 2);
            }
        }
    }

    private static boolean facesCenterSide(int dx, int dz, BlockPos center, BlockPos pod) {
        int tx = Integer.signum(center.getX() - pod.getX());
        int tz = Integer.signum(center.getZ() - pod.getZ());
        return (dx == tx && dz == 0) || (dz == tz && dx == 0);
    }
}
