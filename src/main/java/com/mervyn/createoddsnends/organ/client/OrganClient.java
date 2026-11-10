package com.mervyn.createoddsnends.organ.client;

import com.mervyn.createoddsnends.Tracer;
import com.mervyn.createoddsnends.organ.Organ;
import com.mervyn.createoddsnends.organ.ScoreSync;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(modid = Tracer.MODID, value = Dist.CLIENT)
public final class OrganClient {
    private OrganClient() {}

    @SubscribeEvent
    static void clientSetup(FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            ScoreSync.clearClient();
            BookIconModel.clearAll();
        });
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Organ.BARREL_ORGAN_BE.get(), BarrelOrganRenderer::new);
        event.registerBlockEntityRenderer(Organ.WIND_CHEST_BE.get(), WindChestRenderer::new);
    }

    @SubscribeEvent
    static void iconModels(ModelEvent.ModifyBakingResult event) {
        event.getModels().computeIfPresent(ModelResourceLocation.inventory(Organ.BOOK_MUSIC.getId()), (key, base) -> new BookIconModel(base));
    }
}
