package com.arlight.bosses.client;

import com.arlight.bosses.block.entity.MedalPedestalBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * Render GeckoLib único. La medalla forma parte del modelo del pedestal, evitando
 * el render recursivo de un GeoItem que en algunos clientes Arclight no aparecía.
 */
public final class MedalPedestalRenderer extends GeoBlockRenderer<MedalPedestalBlockEntity> {
    public MedalPedestalRenderer(BlockEntityRendererProvider.Context context) {
        super(new MedalPedestalModel());
    }
}
