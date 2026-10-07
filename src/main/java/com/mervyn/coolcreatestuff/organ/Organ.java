package com.mervyn.coolcreatestuff.organ;

import com.mervyn.coolcreatestuff.Tracer;
import com.mojang.serialization.Codec;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Organ {
    static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Tracer.MODID);
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Tracer.MODID);
    static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Tracer.MODID);
    static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Tracer.MODID);
    static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Tracer.MODID);
    static final DeferredRegister<ArmInteractionPointType> ARM_POINTS = DeferredRegister.create(CreateRegistries.ARM_INTERACTION_POINT_TYPE, Tracer.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SCORE = COMPONENTS.registerComponentType("score",
            b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    private static BlockBehaviour.Properties cabinet() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(2f).sound(SoundType.WOOD).noOcclusion().pushReaction(PushReaction.BLOCK);
    }

    public static final DeferredBlock<BarrelOrganBlock> BARREL_ORGAN = BLOCKS.register("barrel_organ", () -> new BarrelOrganBlock(cabinet()));
    public static final DeferredBlock<BarrelOrganTopBlock> BARREL_ORGAN_TOP = BLOCKS.register("barrel_organ_top", () -> new BarrelOrganTopBlock(cabinet().noLootTable()));
    public static final DeferredBlock<WindChestBlock> WIND_CHEST = BLOCKS.register("wind_chest", () -> new WindChestBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2f).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<WindLineBlock> WIND_LINE = BLOCKS.register("wind_line", () -> new WindLineBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1.5f).sound(SoundType.COPPER).noOcclusion()));

    public static final DeferredItem<BlockItem> BARREL_ORGAN_ITEM = ITEMS.registerSimpleBlockItem(BARREL_ORGAN);
    public static final DeferredItem<BlockItem> WIND_CHEST_ITEM = ITEMS.registerSimpleBlockItem(WIND_CHEST);
    public static final DeferredItem<BlockItem> WIND_LINE_ITEM = ITEMS.registerSimpleBlockItem(WIND_LINE);
    public static final DeferredItem<BookMusicItem> BOOK_MUSIC = ITEMS.registerItem("book_music", BookMusicItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BarrelOrganBlockEntity>> BARREL_ORGAN_BE = BLOCK_ENTITIES.register("barrel_organ",
            () -> BlockEntityType.Builder.of((pos, state) -> new BarrelOrganBlockEntity(Organ.BARREL_ORGAN_BE.get(), pos, state), BARREL_ORGAN.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WindChestBlockEntity>> WIND_CHEST_BE = BLOCK_ENTITIES.register("wind_chest",
            () -> BlockEntityType.Builder.of((pos, state) -> new WindChestBlockEntity(Organ.WIND_CHEST_BE.get(), pos, state), WIND_CHEST.get()).build(null));

    public static final DeferredHolder<ArmInteractionPointType, OrganArmPoint.Type> ORGAN_ARM_POINT = ARM_POINTS.register("barrel_organ", OrganArmPoint.Type::new);

    public static final DeferredHolder<SoundEvent, SoundEvent> MELODY_LOW = sound("organ.melody.low");
    public static final DeferredHolder<SoundEvent, SoundEvent> MELODY_HIGH = sound("organ.melody.high");
    public static final DeferredHolder<SoundEvent, SoundEvent> BASS_LOW = sound("organ.bass.low");
    public static final DeferredHolder<SoundEvent, SoundEvent> BASS_HIGH = sound("organ.bass.high");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHORD_LOW = sound("organ.chord.low");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHORD_HIGH = sound("organ.chord.high");
    public static final DeferredHolder<SoundEvent, SoundEvent> KICK = sound("organ.kick");
    public static final DeferredHolder<SoundEvent, SoundEvent> SNARE = sound("organ.snare");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Tracer.MODID, name)));
    }

    private Organ() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        COMPONENTS.register(modBus);
        SOUNDS.register(modBus);
        ARM_POINTS.register(modBus);
        modBus.addListener(ScoreSync::register);
        modBus.addListener(Organ::registerCapabilities);
        modBus.addListener(Organ::fillTab);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player)
                ScoreSync.forget(player);
        });
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, be, side) -> BarrelOrganBlockEntity.books(level, pos),
                BARREL_ORGAN.get(), BARREL_ORGAN_TOP.get());
    }

    private static void fillTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != Tracer.TAB.getKey())
            return;
        event.accept(BARREL_ORGAN_ITEM.get());
        event.accept(WIND_CHEST_ITEM.get());
        event.accept(WIND_LINE_ITEM.get());
    }
}
