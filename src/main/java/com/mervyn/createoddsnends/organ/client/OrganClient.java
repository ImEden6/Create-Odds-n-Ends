package com.mervyn.createoddsnends.organ.client;

import com.mervyn.createoddsnends.Tracer;
import com.mervyn.createoddsnends.organ.Organ;
import com.mervyn.createoddsnends.organ.ScoreSync;

import net.minecraft.client.Minecraft;
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
public final class OrganClient {
    private static boolean soundsPaused;

    private OrganClient() {}

    @SubscribeEvent
    static void clientSetup(FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            ScoreSync.clearClient();
            BookIconModel.clearAll();
        });
        // a pausing screen like Ponder stops sounds ticking but not playing, so held notes never got
        // their fade and whistles kept blowing; pause what's playing and pick it back up after
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.isPaused() == soundsPaused)
                return;
            soundsPaused = mc.isPaused();
            if (soundsPaused)
                mc.getSoundManager().pause();
            else
                mc.getSoundManager().resume();
        });
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Organ.BARREL_ORGAN_BE.get(), BarrelOrganRenderer::new);
        event.registerBlockEntityRenderer(Organ.WIND_CHEST_BE.get(), WindChestRenderer::new);
        event.registerBlockEntityRenderer(Organ.INSTRUMENT_BE.get(), InstrumentRenderer::new);
    }

    @SubscribeEvent
    static void iconModels(ModelEvent.ModifyBakingResult event) {
        event.getModels().computeIfPresent(ModelResourceLocation.inventory(Organ.BOOK_MUSIC.getId()), (key, base) -> new BookIconModel(base));
    }
}
