package com.arlight.bosses;

import com.arlight.bosses.client.BossClientEvents;
import com.arlight.bosses.block.BossBlocks;
import com.arlight.bosses.block.entity.BossBlockEntities;
import com.arlight.bosses.entity.BossEntities;
import com.arlight.bosses.item.BossItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

@Mod(ArlightBosses.MOD_ID)
public final class ArlightBosses {
    public static final String MOD_ID = "arlightbosses";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ArlightBosses(IEventBus modBus, ModContainer container) {
        BossBlocks.register(modBus);
        BossBlockEntities.register(modBus);
        BossEntities.register(modBus);
        modBus.addListener(BossEntities::registerAttributes);
        BossItems.register(modBus);
        NeoForge.EVENT_BUS.register(new BossRuntimeEvents());
        if (FMLEnvironment.dist == Dist.CLIENT) BossClientEvents.register(modBus);
    }
}
