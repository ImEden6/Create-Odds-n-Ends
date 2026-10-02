package com.mervyn.coolcreatestuff.ponder;

import static com.mervyn.coolcreatestuff.lathe.LathePartBlock.FORMED;

import com.mervyn.coolcreatestuff.Profile;
import com.mervyn.coolcreatestuff.Tracer;
import com.mervyn.coolcreatestuff.turned.TurnedBlockItem;
import com.mervyn.coolcreatestuff.lathe.HeadstockBlockEntity;
import com.mervyn.coolcreatestuff.turned.TurnedBlockEntity;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;

// All lathe schematics have the headstock at (1, 2, 3) facing east, so the bed runs along +x, the
// operator side faces the camera and a tracer row sits at z 1. Parts are saved unformed and the
// scenes form them.
public final class LatheScenes {

    private LatheScenes() {}

    public static void build(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("lathe_build", "Building the Lathe");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        BlockPos headstock = util.grid().at(1, 2, 3);
        BlockPos slot = util.grid().at(2, 2, 3);
        BlockPos tailstock = util.grid().at(3, 2, 3);
        Selection motor = util.select().position(0, 2, 3);
        Selection kinetics = util.select().position(0, 2, 3).add(util.select().position(headstock));
        Selection lathe = util.select().fromTo(1, 1, 3, 3, 2, 3);
        scene.world().setKineticSpeed(kinetics, 0);
        scene.idle(10);

        scene.world().showSection(util.select().fromTo(1, 1, 3, 3, 1, 3), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(60)
                .attachKeyFrame()
                .text("A lathe sits on three lathe beds in a row")
                .pointAt(util.vector().topOf(util.grid().at(2, 1, 3)))
                .placeNearTarget();
        scene.idle(70);

        scene.world().showSection(util.select().position(headstock), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(tailstock), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(70)
                .text("Headstock on one end, tailstock on the other. Place them looking along the bed so the headstock ends up on your left")
                .pointAt(util.vector().topOf(headstock))
                .placeNearTarget();
        scene.idle(80);

        scene.world().showSection(util.select().position(slot), Direction.DOWN);
        scene.world().setBlock(slot, Blocks.COBBLESTONE.defaultBlockState(), false);
        scene.idle(10);
        scene.overlay().showControls(util.vector().topOf(headstock), Pointing.DOWN, 30).rightClick().withItem(AllItems.WRENCH.asStack());
        scene.idle(10);
        scene.overlay().showOutline(PonderPalette.RED, "slot", util.select().position(slot), 70);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .colored(PonderPalette.RED)
                .text("The spot between them has to stay empty. If anything is off, the wrench fails and tells you where")
                .pointAt(util.vector().centerOf(slot))
                .placeNearTarget();
        scene.idle(80);

        scene.world().destroyBlock(slot);
        scene.idle(15);
        scene.overlay().showControls(util.vector().topOf(headstock), Pointing.DOWN, 30).rightClick().withItem(AllItems.WRENCH.asStack());
        scene.idle(10);
        form(scene, lathe, headstock);
        scene.effects().indicateSuccess(headstock);
        scene.overlay().showText(60)
                .attachKeyFrame()
                .colored(PonderPalette.GREEN)
                .text("With the middle clear, a wrench on any part puts it together")
                .pointAt(util.vector().topOf(headstock))
                .placeNearTarget();
        scene.idle(70);

        scene.world().showSection(motor, Direction.EAST);
        scene.idle(10);
        scene.world().setKineticSpeed(kinetics, 32);
        scene.overlay().showText(60)
                .text("Rotation goes into the back of the headstock")
                .pointAt(util.vector().blockSurface(headstock, Direction.WEST))
                .placeNearTarget();
        scene.idle(70);
        scene.overlay().showText(60)
                .text("Breaking any part takes the whole lathe apart again")
                .pointAt(util.vector().topOf(tailstock))
                .placeNearTarget();
        scene.idle(70);
    }

    public static void cutting(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("lathe_cutting", "Turning Blocks on the Lathe");
        scene.configureBasePlate(0, 0, 5);

        BlockPos headstock = util.grid().at(1, 2, 3);
        BlockPos slot = util.grid().at(2, 2, 3);
        Selection kinetics = util.select().position(0, 2, 3).add(util.select().position(headstock));
        form(scene, util.select().fromTo(1, 1, 3, 3, 2, 3), headstock);
        scene.world().setKineticSpeed(kinetics, 0);
        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(15);

        ItemStack template = new ItemStack(Tracer.PROFILE_TEMPLATE.get());
        template.set(Tracer.PROFILE.get(), Profile.BALUSTER);
        Vec3 front = util.vector().blockSurface(slot, Direction.SOUTH);
        scene.overlay().showControls(front, Pointing.DOWN, 40).rightClick().withItem(template);
        scene.idle(7);
        put(scene, util, headstock, "Template", template);
        scene.overlay().showText(60)
                .attachKeyFrame()
                .text("Right-click the lathe with a written profile template")
                .pointAt(front)
                .placeNearTarget();
        scene.idle(70);

        ItemStack log = new ItemStack(Blocks.OAK_LOG);
        scene.overlay().showControls(front, Pointing.DOWN, 40).rightClick().withItem(log);
        scene.idle(7);
        put(scene, util, headstock, "Input", log);
        scene.overlay().showText(60)
                .text("Then with the block you want turned. Most full blocks work")
                .pointAt(util.vector().centerOf(slot))
                .placeNearTarget();
        scene.idle(70);

        scene.world().setKineticSpeed(kinetics, 64);
        startCut(scene, util, headstock);
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("Give it rotation and the cutter works along the piece, following the template")
                .pointAt(util.vector().topOf(slot))
                .placeNearTarget();
        scene.idle(130);

        finishCut(scene, util, headstock, Profile.BALUSTER, Blocks.OAK_LOG);
        scene.world().setKineticSpeed(kinetics, 0);
        scene.idle(10);
        scene.overlay().showControls(front, Pointing.DOWN, 40).rightClick();
        scene.overlay().showText(60)
                .attachKeyFrame()
                .text("Take the finished piece out with an empty hand. Sneak to take the template back")
                .pointAt(front)
                .placeNearTarget();
        scene.idle(70);
        scene.overlay().showText(70)
                .text("Funnels, hoppers and mechanical arms can feed blocks in and pull pieces out from any lathe part")
                .pointAt(util.vector().topOf(util.grid().at(3, 2, 3)))
                .placeNearTarget();
        scene.idle(80);
    }

    public static void tracer(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("lathe_tracer", "Copying a Master with the Tracer");
        scene.configureBasePlate(0, 0, 5);

        BlockPos headstock = util.grid().at(1, 2, 3);
        BlockPos slot = util.grid().at(2, 2, 3);
        BlockPos holder = util.grid().at(1, 2, 1);
        BlockPos masterSlot = util.grid().at(2, 2, 1);
        Selection lathe = util.select().fromTo(0, 1, 3, 3, 2, 3);
        Selection tracerRow = util.select().fromTo(1, 1, 1, 3, 2, 1);
        Selection kinetics = util.select().position(0, 2, 3).add(util.select().position(headstock));
        form(scene, util.select().fromTo(1, 1, 3, 3, 2, 3), headstock);
        scene.world().setKineticSpeed(kinetics, 0);
        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(lathe, Direction.DOWN);
        scene.idle(15);

        scene.world().showSection(util.select().fromTo(1, 1, 1, 3, 1, 1), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(holder).add(util.select().position(3, 2, 1)), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("A tracer row goes two blocks behind the lathe: three rails, a holder over each end, the middle left empty")
                .pointAt(util.vector().topOf(masterSlot.below()))
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showControls(util.vector().topOf(headstock), Pointing.DOWN, 30).rightClick().withItem(AllItems.WRENCH.asStack());
        scene.idle(10);
        form(scene, tracerRow, headstock);
        scene.world().modifyBlockEntityNBT(util.select().position(headstock), HeadstockBlockEntity.class, nbt -> nbt.putBoolean("Tracer", true));
        scene.effects().indicateSuccess(holder);
        scene.overlay().showText(60)
                .text("Wrench the lathe again and the tracer joins it")
                .pointAt(util.vector().topOf(holder))
                .placeNearTarget();
        scene.idle(70);

        ItemStack master = TurnedBlockItem.of(Profile.VASE, Blocks.SPRUCE_PLANKS);
        scene.overlay().showControls(util.vector().topOf(holder), Pointing.DOWN, 40).rightClick().withItem(master);
        scene.idle(7);
        put(scene, util, headstock, "Master", master);
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("Right-click a holder with any turned piece to load it as the master. It wins over the template")
                .pointAt(util.vector().centerOf(masterSlot))
                .placeNearTarget();
        scene.idle(90);

        Vec3 front = util.vector().blockSurface(slot, Direction.SOUTH);
        ItemStack stone = new ItemStack(Blocks.STONE);
        scene.overlay().showControls(front, Pointing.DOWN, 40).rightClick().withItem(stone);
        scene.idle(7);
        put(scene, util, headstock, "Input", stone);
        scene.world().setKineticSpeed(kinetics, 64);
        startCut(scene, util, headstock);
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("The stylus rides the master and the cutter copies it, no template needed")
                .pointAt(util.vector().centerOf(masterSlot))
                .placeNearTarget();
        scene.idle(120);

        finishCut(scene, util, headstock, Profile.VASE, Blocks.STONE);
        scene.world().setKineticSpeed(kinetics, 0);
        scene.idle(10);
        scene.overlay().showText(80)
                .text("Pull the master out mid cut and the lathe just waits, keeping its progress until a master or template is back")
                .pointAt(util.vector().topOf(holder))
                .placeNearTarget();
        scene.idle(90);
        scene.overlay().showText(60)
                .text("Funnels on the tracer row load masters too")
                .pointAt(util.vector().topOf(util.grid().at(3, 1, 1)))
                .placeNearTarget();
        scene.idle(70);
    }

    public static void templateTable(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("template_table", "Writing Profile Templates");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        BlockPos table = util.grid().at(1, 1, 2);
        BlockPos piece = util.grid().at(3, 1, 2);
        scene.world().showSection(util.select().position(table), Direction.DOWN);
        scene.idle(15);

        ItemStack blank = new ItemStack(Tracer.PROFILE_TEMPLATE.get());
        scene.overlay().showControls(util.vector().topOf(table), Pointing.DOWN, 40).rightClick();
        scene.idle(7);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("The template table writes shapes onto profile templates. Put a template in the slot")
                .pointAt(util.vector().topOf(table))
                .placeNearTarget();
        scene.idle(80);
        scene.overlay().showText(80)
                .text("Each of the 16 bars is one layer, foot at the left. Drag them to set how wide the piece is there, then hit Save")
                .pointAt(util.vector().topOf(table))
                .placeNearTarget();
        scene.idle(90);

        scene.world().setBlock(piece, Tracer.TURNED_BLOCK.get().defaultBlockState(), false);
        scene.world().modifyBlockEntity(piece, TurnedBlockEntity.class, be -> be.setShape(Profile.BALUSTER, Blocks.OAK_PLANKS));
        scene.world().showSection(util.select().position(piece), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showControls(util.vector().topOf(piece), Pointing.DOWN, 40).rightClick().whileSneaking().withItem(blank);
        scene.idle(7);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("Or sneak-click a turned piece with a template to copy its shape")
                .pointAt(util.vector().topOf(piece))
                .placeNearTarget();
        scene.idle(80);
    }

    // Ponder keeps the old block entity when a state changes, along with its cached state
    private static void form(CreateSceneBuilder scene, Selection parts, BlockPos headstock) {
        scene.world().modifyBlocks(parts, state -> state.hasProperty(FORMED) ? state.setValue(FORMED, true) : state, false);
        scene.world().modifyBlockEntity(headstock, HeadstockBlockEntity.class, be -> be.setBlockState(be.getLevel().getBlockState(headstock)));
    }

    private static void put(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos headstock, String slot, ItemStack stack) {
        HolderLookup.Provider registries = scene.world().getHolderLookupProvider();
        ItemStackHandler handler = new ItemStackHandler(1);
        handler.setStackInSlot(0, stack.copy());
        scene.world().modifyBlockEntityNBT(util.select().position(headstock), HeadstockBlockEntity.class,
                nbt -> nbt.put(slot, handler.serializeNBT(registries)));
    }

    // Ponder runs the client side of the block entity, which counts progress along but never finishes
    private static void startCut(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos headstock) {
        scene.world().modifyBlockEntityNBT(util.select().position(headstock), HeadstockBlockEntity.class, nbt -> {
            nbt.putBoolean("Cutting", true);
            nbt.putFloat("Progress", 0);
        });
    }

    private static void finishCut(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos headstock, Profile profile, Block source) {
        put(scene, util, headstock, "Input", ItemStack.EMPTY);
        put(scene, util, headstock, "Output", TurnedBlockItem.of(profile, source));
        scene.world().modifyBlockEntityNBT(util.select().position(headstock), HeadstockBlockEntity.class, nbt -> {
            nbt.putBoolean("Cutting", false);
            nbt.putFloat("Progress", 0);
        });
    }
}
