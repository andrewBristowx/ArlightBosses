package com.arlight.bosses.client;

import com.arlight.bosses.block.entity.MedalPedestalBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class MedalPedestalRenderer extends GeoBlockRenderer<MedalPedestalBlockEntity> {
    public MedalPedestalRenderer(BlockEntityRendererProvider.Context context) {
        super(new MedalPedestalModel());
    }
}
