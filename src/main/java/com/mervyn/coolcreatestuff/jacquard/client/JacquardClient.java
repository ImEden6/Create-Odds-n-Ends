package com.mervyn.coolcreatestuff.jacquard.client;

import com.mervyn.coolcreatestuff.Tracer;
import com.mervyn.coolcreatestuff.jacquard.DesignSync;
import com.mervyn.coolcreatestuff.jacquard.Jacquard;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(modid = Tracer.MODID, value = Dist.CLIENT)
public final class JacquardClient {
    private static int ticks;

    private JacquardClient() {}

    @SubscribeEvent
    static void clientSetup(FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            if (++ticks % 20 == 0)
                DesignTextures.releaseUnused();
        });
        if (Showcase.ENABLED)
            NeoForge.EVENT_BUS.addListener(Showcase::tick);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            DesignSync.clearClient();
            DesignTextures.clear();
            DesignIconModel.clearAll();
        });
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Jacquard.JACQUARD_HEAD_BE.get(), LoomRenderer::new);
        event.registerBlockEntityRenderer(Jacquard.LOOM_DRIVE_BE.get(), LoomDriveRenderer::new);
        event.registerBlockEntityRenderer(Jacquard.CARD_PUNCH_BE.get(), CardPunchRenderer::new);
        event.registerEntityRenderer(Jacquard.TAPESTRY_ENTITY.get(), TapestryRenderer::new);
    }

    @SubscribeEvent
    static void iconModels(ModelEvent.ModifyBakingResult event) {
        DesignIconModel.forgetModels();
        event.getModels().computeIfPresent(ModelResourceLocation.inventory(Jacquard.PUNCHED_CARD_CHAIN.getId()), (key, base) -> new DesignIconModel(base, DesignIconModel::chain));
        event.getModels().computeIfPresent(ModelResourceLocation.inventory(Jacquard.TAPESTRY.getId()), (key, base) -> new DesignIconModel(base, DesignIconModel::thumbnail));
    }
}
