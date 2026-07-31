package com.arlight.bosses;

import com.arlight.bosses.entity.minion.EmeraldCorruptionArrow;
import com.arlight.bosses.entity.minion.GildedShardProjectile;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/** Limpieza defensiva para servidores híbridos Arclight/NeoForge. */
public final class BossRuntimeEvents {
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        int removed = 0;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            List<Entity> temporary = new ArrayList<>();
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof EmeraldCorruptionArrow || entity instanceof GildedShardProjectile) {
                    temporary.add(entity);
                }
            }
            for (Entity entity : temporary) {
                entity.discard();
                removed++;
            }
        }
        if (removed > 0) {
            ArlightBosses.LOGGER.info("ArlightBosses retiró {} proyectiles temporales antes de guardar el servidor.", removed);
        }
    }
}
