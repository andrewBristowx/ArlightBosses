package com.arlight.tetris.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class TetrisArenaConfig {
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("arlighttetris-arena.txt");
    private TetrisArenaConfig() {}
    public record ArenaLocation(ResourceKey<Level> dimension, BlockPos center) {}

    public static void save(ArenaLocation location) {
        String line = location.dimension().location() + ";" + location.center().getX()
                + ";" + location.center().getY() + ";" + location.center().getZ();
        try { Files.writeString(FILE, line, StandardCharsets.UTF_8); }
        catch (IOException e) { throw new RuntimeException("No se pudo guardar la arena", e); }
    }

    public static Optional<ArenaLocation> load() {
        try {
            if (!Files.exists(FILE)) return Optional.empty();
            String[] p = Files.readString(FILE, StandardCharsets.UTF_8).trim().split(";");
            if (p.length != 4) return Optional.empty();
            ResourceKey<Level> dim = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                    ResourceLocation.parse(p[0]));
            return Optional.of(new ArenaLocation(dim,
                    new BlockPos(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]))));
        } catch (Exception e) { return Optional.empty(); }
    }
}
