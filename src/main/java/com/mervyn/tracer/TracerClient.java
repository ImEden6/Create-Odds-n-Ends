package com.mervyn.tracer;

import com.mervyn.tracer.client.HeadstockRenderer;
import com.mervyn.tracer.client.LathePartials;
import com.mervyn.tracer.client.LatheVisual;
import com.mervyn.tracer.client.TemplateTableScreen;
import com.mervyn.tracer.client.TurnedModel;
import com.mervyn.tracer.client.TurnedQuads;

import com.mervyn.tracer.turned.TurnedBlockEntity;

import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Tracer.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Tracer.MODID, value = Dist.CLIENT)
public class TracerClient {
    public TracerClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        LathePartials.init();
    }

    @SubscribeEvent
    static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> SimpleBlockEntityVisualizer.builder(Tracer.LATHE_HEADSTOCK_BE.get())
                .factory(LatheVisual::create)
                .skipVanillaRender(be -> true)
                .apply());
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Tracer.LATHE_HEADSTOCK_BE.get(), HeadstockRenderer::new);
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(Tracer.TEMPLATE_TABLE_MENU.get(), TemplateTableScreen::new);
    }

    // grass, leaves and the like are grey in their textures; the tint has to come from the source block
    @SubscribeEvent
    static void blockColors(RegisterColorHandlersEvent.Block event) {
        BlockColors colors = event.getBlockColors();
        event.register((state, level, pos, tint) -> level != null && pos != null && level.getBlockEntity(pos) instanceof TurnedBlockEntity be
                ? colors.getColor(be.getSource().defaultBlockState(), level, pos, tint) : -1, Tracer.TURNED_BLOCK.get());
    }

    @SubscribeEvent
    static void itemColors(RegisterColorHandlersEvent.Item event) {
        ItemColors colors = event.getItemColors();
        event.register((stack, tint) -> colors.getColor(new ItemStack(stack.getOrDefault(Tracer.SOURCE_BLOCK.get(), Blocks.OAK_PLANKS)), tint),
                Tracer.TURNED_BLOCK_ITEM.get());
    }

    @SubscribeEvent
    static void swapTurnedModels(ModelEvent.ModifyBakingResult event) {
        for (BlockState state : Tracer.TURNED_BLOCK.get().getStateDefinition().getPossibleStates())
            event.getModels().computeIfPresent(BlockModelShaper.stateToModelLocation(state), (key, base) -> new TurnedModel(base));
        event.getModels().computeIfPresent(ModelResourceLocation.inventory(Tracer.TURNED_BLOCK.getId()), (key, base) -> new TurnedModel(base));
    }

    @SubscribeEvent
    static void clearTurnedQuads(ModelEvent.BakingCompleted event) {
        TurnedQuads.clear();
    }
}
