package com.arlight.bosses.client;

import com.arlight.bosses.entity.*;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Dedicated guardian models: surface, Nether, void and Somita vampire (draconic slot). */
public final class GuardianModel<T extends GuardianEntity> extends GeoModel<T> {
    @Override
    public ResourceLocation getModelResource(T guardian) {
        return id("geo/" + asset(guardian) + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T guardian) {
        return id("textures/entity/" + asset(guardian) + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(T guardian) {
        return id("animations/" + asset(guardian) + ".animation.json");
    }

    private static String asset(GuardianEntity guardian) {
        if (guardian instanceof NetherGuardian) return "nether_guardian";
        if (guardian instanceof VoidGuardian) return "void_guardian";
        if (guardian instanceof DragonGuardian) return "somita_vampire_guardian";
        return "surface_guardian";
    }

    private static ResourceLocation id(String path) { return BossClientEvents.id(path); }
}
