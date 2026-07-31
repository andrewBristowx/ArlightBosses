package com.arlight.bosses.client;

import com.arlight.bosses.block.entity.NetherDungeonLockBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public final class NetherDungeonLockRenderer extends GeoBlockRenderer<NetherDungeonLockBlockEntity> {
    public NetherDungeonLockRenderer(BlockEntityRendererProvider.Context context) {
        super(new NetherDungeonLockModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
