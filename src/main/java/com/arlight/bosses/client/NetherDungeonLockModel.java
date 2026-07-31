package com.arlight.bosses.client;

import com.arlight.bosses.block.entity.NetherDungeonLockBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class NetherDungeonLockModel extends GeoModel<NetherDungeonLockBlockEntity> {
    @Override public ResourceLocation getModelResource(NetherDungeonLockBlockEntity animatable) {
        return BossClientEvents.id("geo/nether_dungeon_lock.geo.json");
    }
    @Override public ResourceLocation getTextureResource(NetherDungeonLockBlockEntity animatable) {
        return BossClientEvents.id("textures/block/nether_dungeon_lock.png");
    }
    @Override public ResourceLocation getAnimationResource(NetherDungeonLockBlockEntity animatable) {
        return BossClientEvents.id("animations/nether_dungeon_lock.animation.json");
    }
}
