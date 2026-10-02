package com.mervyn.coolcreatestuff;

import org.slf4j.Logger;

import com.mervyn.coolcreatestuff.lathe.HeadstockBlock;
import com.mervyn.coolcreatestuff.lathe.HeadstockBlockEntity;
import com.mervyn.coolcreatestuff.lathe.LatheArmPoint;
import com.mervyn.coolcreatestuff.lathe.LatheItems;
import com.mervyn.coolcreatestuff.lathe.LathePartBlock;
import com.mervyn.coolcreatestuff.template.ProfileTemplateItem;
import com.mervyn.coolcreatestuff.template.SaveProfile;
import com.mervyn.coolcreatestuff.template.TemplateTableBlock;
import com.mervyn.coolcreatestuff.template.TemplateTableMenu;
import com.mervyn.coolcreatestuff.turned.TurnedBlock;
import com.mervyn.coolcreatestuff.turned.TurnedBlockEntity;
import com.mervyn.coolcreatestuff.turned.TurnedBlockItem;
import com.mojang.logging.LogUtils;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(Tracer.MODID)
public class Tracer {
    public static final String MODID = "tracer";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<ArmInteractionPointType> ARM_POINTS = DeferredRegister.create(CreateRegistries.ARM_INTERACTION_POINT_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Profile>> PROFILE = COMPONENTS.registerComponentType("profile",
            b -> b.persistent(Profile.CODEC).networkSynchronized(Profile.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Block>> SOURCE_BLOCK = COMPONENTS.registerComponentType("source_block",
            b -> b.persistent(BuiltInRegistries.BLOCK.byNameCodec()).networkSynchronized(ByteBufCodecs.registry(Registries.BLOCK)));

    private static BlockBehaviour.Properties machine() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PODZOL).strength(1.5f).sound(SoundType.WOOD).noOcclusion();
    }

    public static final DeferredBlock<HeadstockBlock> LATHE_HEADSTOCK = BLOCKS.register("lathe_headstock", () -> new HeadstockBlock(machine()));
    public static final DeferredBlock<LathePartBlock> LATHE_BED = BLOCKS.register("lathe_bed", () -> new LathePartBlock(machine(), LathePartBlock.BED));
    public static final DeferredBlock<LathePartBlock> LATHE_TAILSTOCK = BLOCKS.register("lathe_tailstock", () -> new LathePartBlock(machine(), LathePartBlock.TAILSTOCK));
    public static final DeferredBlock<LathePartBlock> TRACER_RAIL = BLOCKS.register("tracer_rail", () -> new LathePartBlock(machine(), LathePartBlock.RAIL));
    public static final DeferredBlock<LathePartBlock> TRACER_HOLDER = BLOCKS.register("tracer_holder", () -> new LathePartBlock(machine(), LathePartBlock.HOLDER));
    public static final DeferredBlock<TurnedBlock> TURNED_BLOCK = BLOCKS.register("turned_block", () -> new TurnedBlock(
            BlockBehaviour.Properties.of().strength(1.5f).noOcclusion().dynamicShape()));

    public static final DeferredBlock<TemplateTableBlock> TEMPLATE_TABLE = BLOCKS.register("template_table", () -> new TemplateTableBlock(machine()));

    public static final DeferredItem<BlockItem> LATHE_HEADSTOCK_ITEM = ITEMS.registerSimpleBlockItem(LATHE_HEADSTOCK);
    public static final DeferredItem<BlockItem> LATHE_BED_ITEM = ITEMS.registerSimpleBlockItem(LATHE_BED);
    public static final DeferredItem<BlockItem> LATHE_TAILSTOCK_ITEM = ITEMS.registerSimpleBlockItem(LATHE_TAILSTOCK);
    public static final DeferredItem<BlockItem> TRACER_RAIL_ITEM = ITEMS.registerSimpleBlockItem(TRACER_RAIL);
    public static final DeferredItem<BlockItem> TRACER_HOLDER_ITEM = ITEMS.registerSimpleBlockItem(TRACER_HOLDER);
    public static final DeferredItem<TurnedBlockItem> TURNED_BLOCK_ITEM = ITEMS.registerItem("turned_block", p -> new TurnedBlockItem(TURNED_BLOCK.get(), p));
    public static final DeferredItem<BlockItem> TEMPLATE_TABLE_ITEM = ITEMS.registerSimpleBlockItem(TEMPLATE_TABLE);
    public static final DeferredItem<ProfileTemplateItem> PROFILE_TEMPLATE = ITEMS.registerItem("profile_template", ProfileTemplateItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredHolder<MenuType<?>, MenuType<TemplateTableMenu>> TEMPLATE_TABLE_MENU = MENUS.register("template_table",
            () -> new MenuType<>(TemplateTableMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeadstockBlockEntity>> LATHE_HEADSTOCK_BE = BLOCK_ENTITIES.register("lathe_headstock",
            () -> BlockEntityType.Builder.of((pos, state) -> new HeadstockBlockEntity(Tracer.LATHE_HEADSTOCK_BE.get(), pos, state), LATHE_HEADSTOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TurnedBlockEntity>> TURNED_BLOCK_BE = BLOCK_ENTITIES.register("turned_block",
            () -> BlockEntityType.Builder.of(TurnedBlockEntity::new, TURNED_BLOCK.get()).build(null));

    public static final DeferredHolder<ArmInteractionPointType, LatheArmPoint.Type> LATHE_ARM_POINT = ARM_POINTS.register("lathe", LatheArmPoint.Type::new);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_MODE_TABS.register("tracer", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tracer"))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .icon(() -> LATHE_HEADSTOCK_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(LATHE_HEADSTOCK_ITEM.get());
                output.accept(LATHE_BED_ITEM.get());
                output.accept(LATHE_TAILSTOCK_ITEM.get());
                output.accept(TRACER_RAIL_ITEM.get());
                output.accept(TRACER_HOLDER_ITEM.get());
                output.accept(TEMPLATE_TABLE_ITEM.get());
                output.accept(PROFILE_TEMPLATE.get());
            }).build());

    public Tracer(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        COMPONENTS.register(modEventBus);
        MENUS.register(modEventBus);
        ARM_POINTS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(Tracer::registerPayloads);
        modEventBus.addListener(Tracer::registerCapabilities);

        NeoForge.EVENT_BUS.addListener(TracerCommands::register);
        NeoForge.EVENT_BUS.addListener(LathePartBlock::keepSlotsClear);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, be, side) -> new LatheItems(level, pos),
                LATHE_HEADSTOCK.get(), LATHE_BED.get(), LATHE_TAILSTOCK.get(), TRACER_RAIL.get(), TRACER_HOLDER.get());
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(SaveProfile.TYPE, SaveProfile.STREAM_CODEC, SaveProfile::handle);
    }
}
