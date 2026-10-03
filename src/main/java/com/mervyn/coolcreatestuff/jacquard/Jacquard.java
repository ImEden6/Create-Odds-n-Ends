package com.mervyn.coolcreatestuff.jacquard;

import com.mervyn.coolcreatestuff.Tracer;
import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// Everything the punch card loom adds. Tracer's bootstrap only calls register().
public final class Jacquard {
    static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Tracer.MODID);
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Tracer.MODID);
    static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Tracer.MODID);
    static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Tracer.MODID);
    static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Tracer.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> DESIGN = COMPONENTS.registerComponentType("design",
            b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    private static BlockBehaviour.Properties wood() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PODZOL).strength(2f).sound(SoundType.WOOD).noOcclusion();
    }

    private static BlockBehaviour.Properties casing() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5f).sound(SoundType.WOOD).noOcclusion();
    }

    public static final DeferredBlock<JacquardHeadBlock> JACQUARD_HEAD = BLOCKS.register("jacquard_head", () -> new JacquardHeadBlock(casing()));
    public static final DeferredBlock<LoomDriveBlock> LOOM_DRIVE = BLOCKS.register("loom_drive", () -> new LoomDriveBlock(casing()));
    public static final DeferredBlock<LoomFrameBlock> LOOM_FRAME = BLOCKS.register("loom_frame", () -> new LoomFrameBlock(wood()));
    public static final DeferredBlock<LoomVoidBlock> LOOM_VOID = BLOCKS.register("loom_void", () -> new LoomVoidBlock(
            BlockBehaviour.Properties.of().noCollission().noLootTable().strength(-1, 3600000).pushReaction(PushReaction.BLOCK).noOcclusion()));
    public static final DeferredBlock<CardPunchBlock> CARD_PUNCH = BLOCKS.register("card_punch", () -> new CardPunchBlock(casing()));

    public static final DeferredItem<BlockItem> JACQUARD_HEAD_ITEM = ITEMS.registerSimpleBlockItem(JACQUARD_HEAD);
    public static final DeferredItem<BlockItem> LOOM_DRIVE_ITEM = ITEMS.registerSimpleBlockItem(LOOM_DRIVE);
    public static final DeferredItem<BlockItem> LOOM_FRAME_ITEM = ITEMS.registerSimpleBlockItem(LOOM_FRAME);
    public static final DeferredItem<BlockItem> CARD_PUNCH_ITEM = ITEMS.registerSimpleBlockItem(CARD_PUNCH);
    public static final DeferredItem<Item> BLANK_CARD = ITEMS.registerSimpleItem("blank_card");
    public static final DeferredItem<DesignCardItem> PUNCHED_CARD_CHAIN = ITEMS.registerItem("punched_card_chain", DesignCardItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<TapestryItem> TAPESTRY = ITEMS.registerItem("tapestry", TapestryItem::new, new Item.Properties().stacksTo(16));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<JacquardHeadBlockEntity>> JACQUARD_HEAD_BE = BLOCK_ENTITIES.register("jacquard_head",
            () -> BlockEntityType.Builder.of((pos, state) -> new JacquardHeadBlockEntity(Jacquard.JACQUARD_HEAD_BE.get(), pos, state), JACQUARD_HEAD.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LoomDriveBlockEntity>> LOOM_DRIVE_BE = BLOCK_ENTITIES.register("loom_drive",
            () -> BlockEntityType.Builder.of((pos, state) -> new LoomDriveBlockEntity(Jacquard.LOOM_DRIVE_BE.get(), pos, state), LOOM_DRIVE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CardPunchBlockEntity>> CARD_PUNCH_BE = BLOCK_ENTITIES.register("card_punch",
            () -> BlockEntityType.Builder.of((pos, state) -> new CardPunchBlockEntity(Jacquard.CARD_PUNCH_BE.get(), pos, state), CARD_PUNCH.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<TapestryEntity>> TAPESTRY_ENTITY = ENTITIES.register("tapestry",
            () -> EntityType.Builder.<TapestryEntity>of(TapestryEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f).clientTrackingRange(10).updateInterval(Integer.MAX_VALUE).build("tapestry"));

    private Jacquard() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ENTITIES.register(modBus);
        COMPONENTS.register(modBus);
        modBus.addListener(DesignSync::register);
        modBus.addListener(Jacquard::fillTab);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player)
                DesignSync.forget(player);
        });
    }

    private static void fillTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != Tracer.TAB.getKey())
            return;
        event.accept(JACQUARD_HEAD_ITEM.get());
        event.accept(LOOM_FRAME_ITEM.get());
        event.accept(LOOM_DRIVE_ITEM.get());
        event.accept(CARD_PUNCH_ITEM.get());
        event.accept(BLANK_CARD.get());
    }
}
