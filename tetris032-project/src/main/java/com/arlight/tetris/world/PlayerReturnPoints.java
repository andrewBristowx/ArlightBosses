package com.arlight.tetris.world;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerReturnPoints {
    public record ReturnPoint(ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch) {}
    private static final Map<UUID, ReturnPoint> POINTS = new ConcurrentHashMap<>();
    private PlayerReturnPoints() {}

    public static void capture(ServerPlayer player) {
        POINTS.put(player.getUUID(), new ReturnPoint(player.level().dimension(),
                player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
    }
    public static ReturnPoint get(UUID id) { return POINTS.get(id); }
    public static void clear(UUID id) { POINTS.remove(id); }
}
