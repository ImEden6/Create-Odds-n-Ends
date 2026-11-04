package com.mervyn.createoddsnends.cnc;

import com.mervyn.createoddsnends.Tracer;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Cnc {
    static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Tracer.MODID);
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Tracer.MODID);
    static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Tracer.MODID);
    static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Tracer.MODID);
    static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Tracer.MODID);

    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5f).sound(SoundType.NETHERITE_BLOCK).noOcclusion();
    }

    public static final DeferredBlock<CncFrameBlock> CNC_FRAME = BLOCKS.register("cnc_frame", () -> new CncFrameBlock(metal()));
    public static final DeferredBlock<CncDriveBlock> CNC_DRIVE = BLOCKS.register("cnc_drive", () -> new CncDriveBlock(metal()));
    public static final DeferredBlock<CncPendantBlock> CNC_PENDANT = BLOCKS.register("cnc_pendant", () -> new CncPendantBlock(metal()));

    public static final DeferredItem<BlockItem> CNC_FRAME_ITEM = ITEMS.registerSimpleBlockItem(CNC_FRAME);
    public static final DeferredItem<BlockItem> CNC_DRIVE_ITEM = ITEMS.registerSimpleBlockItem(CNC_DRIVE);
    public static final DeferredItem<BlockItem> CNC_PENDANT_ITEM = ITEMS.registerSimpleBlockItem(CNC_PENDANT);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CncBlockEntity>> CNC_BE = BLOCK_ENTITIES.register("cnc_pendant",
            () -> BlockEntityType.Builder.of((pos, state) -> new CncBlockEntity(Cnc.CNC_BE.get(), pos, state), CNC_PENDANT.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CncDriveBlockEntity>> CNC_DRIVE_BE = BLOCK_ENTITIES.register("cnc_drive",
            () -> BlockEntityType.Builder.of((pos, state) -> new CncDriveBlockEntity(Cnc.CNC_DRIVE_BE.get(), pos, state), CNC_DRIVE.get()).build(null));

    public static final DeferredHolder<RecipeType<?>, RecipeType<CncRecipe>> CNC_TYPE = RECIPE_TYPES.register("cnc",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(Tracer.MODID, "cnc")));
    public static final DeferredHolder<RecipeSerializer<?>, StandardProcessingRecipe.Serializer<CncRecipe>> CNC_SERIALIZER = RECIPE_SERIALIZERS.register("cnc",
            () -> new StandardProcessingRecipe.Serializer<>(CncRecipe::new));

    static final IRecipeTypeInfo TYPE_INFO = new IRecipeTypeInfo() {
        @Override
        public ResourceLocation getId() {
            return CNC_TYPE.getId();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends RecipeSerializer<?>> T getSerializer() {
            return (T) CNC_SERIALIZER.get();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <I extends RecipeInput, R extends Recipe<I>> RecipeType<R> getType() {
            return (RecipeType<R>) CNC_TYPE.get();
        }
    };

    private Cnc() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        RECIPE_TYPES.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        modBus.addListener(Cnc::registerCapabilities);
        modBus.addListener(Cnc::fillTab);
        NeoForge.EVENT_BUS.addListener(CncDoors::onRightClick);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, be, side) -> new CncItems(level, pos), CNC_FRAME.get(), CNC_DRIVE.get(), CNC_PENDANT.get());
    }

    private static void fillTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != Tracer.TAB.getKey())
            return;
        event.accept(CNC_FRAME_ITEM.get());
        event.accept(CNC_DRIVE_ITEM.get());
        event.accept(CNC_PENDANT_ITEM.get());
    }
}
