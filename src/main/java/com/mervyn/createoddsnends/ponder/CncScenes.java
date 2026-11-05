package com.mervyn.createoddsnends.ponder;

import static com.mervyn.createoddsnends.cnc.CncFrameBlock.FACING;
import static com.mervyn.createoddsnends.cnc.CncFrameBlock.FORMED;
import static com.mervyn.createoddsnends.cnc.CncFrameBlock.PART;

import java.util.List;

import com.mervyn.createoddsnends.cnc.CncBlockEntity;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;

// The cnc schematic has the pendant at (1, 2, 2) facing north toward the camera, frames running
// east of it from x 2 to 5 and back to z 4, the drive at (5, 1, 3) and a creative motor east of it.
// Ponder worlds are client side, so the mill never picks its own job here: the scenes write
// what it would be doing straight into its data.
public final class CncScenes {
    private CncScenes() {}

    private static void form(CreateSceneBuilder scene, SceneBuildingUtil util) {
        for (int a = 0; a < 4; a++)
            for (int u = 0; u < 3; u++)
                for (int b = 0; b < 3; b++) {
                    int part = a + 4 * (u + 3 * b);
                    scene.world().modifyBlock(util.grid().at(2 + a, 1 + u, 2 + b),
                            state -> state.setValue(FORMED, true).setValue(FACING, Direction.NORTH).setValue(PART, part), false);
                }
        BlockPos pendant = util.grid().at(1, 2, 2);
        scene.world().modifyBlock(pendant, state -> state.setValue(FORMED, true), false);
        for (BlockPos pos : new BlockPos[] {pendant, util.grid().at(5, 1, 3)})
            scene.world().modifyBlockEntity(pos, BlockEntity.class, be -> be.setBlockState(be.getLevel().getBlockState(pos)));
    }

    private static void contents(CreateSceneBuilder scene, SceneBuildingUtil util, List<ItemStack> in, List<ItemStack> out, boolean running, ItemStack making) {
        HolderLookup.Provider registries = scene.world().getHolderLookupProvider();
        ItemStackHandler input = new ItemStackHandler(CncBlockEntity.SLOTS);
        ItemStackHandler output = new ItemStackHandler(CncBlockEntity.SLOTS);
        for (int i = 0; i < in.size(); i++)
            input.setStackInSlot(i, in.get(i).copy());
        for (int i = 0; i < out.size(); i++)
            output.setStackInSlot(i, out.get(i).copy());
        scene.world().modifyBlockEntityNBT(util.select().position(1, 2, 2), CncBlockEntity.class, nbt -> {
            nbt.put("Input", input.serializeNBT(registries));
            nbt.put("Output", output.serializeNBT(registries));
            nbt.putBoolean("Running", running);
            nbt.putFloat("Progress", 0);
            nbt.putInt("Duration", 240);
            if (making.isEmpty())
                nbt.remove("Making");
            else
                nbt.put("Making", making.save(registries));
        });
    }

    public static void build(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("cnc_build", "Building the CNC Mill");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos pendant = util.grid().at(1, 2, 2);
        Selection kinetics = util.select().fromTo(5, 1, 3, 6, 1, 3);
        scene.world().setKineticSpeed(kinetics, 0);
        scene.idle(10);

        for (int y = 1; y <= 3; y++) {
            scene.world().showSection(util.select().fromTo(2, y, 2, 5, y, 4), Direction.DOWN);
            scene.idle(8);
        }
        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("The mill is a 4x3x3 box of CNC Frames, with a CNC Drive in the middle of the left end")
                .pointAt(util.vector().topOf(util.grid().at(4, 3, 3)))
                .placeNearTarget();
        scene.idle(80);

        scene.world().showSection(util.select().position(pendant), Direction.EAST);
        scene.idle(10);
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("The CNC Pendant hangs off the right end of the front face, one block up, facing the same way as the doors")
                .pointAt(util.vector().blockSurface(pendant, Direction.NORTH))
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showControls(util.vector().topOf(pendant), Pointing.DOWN, 30).rightClick().withItem(AllItems.WRENCH.asStack());
        scene.idle(10);
        form(scene, util);
        scene.effects().indicateSuccess(pendant);
        scene.overlay().showText(60)
                .attachKeyFrame()
                .colored(PonderPalette.GREEN)
                .text("Wrench the pendant to put it together. If a frame is missing, it tells you where")
                .pointAt(util.vector().topOf(pendant))
                .placeNearTarget();
        scene.idle(70);

        scene.world().showSection(util.select().position(6, 1, 3), Direction.WEST);
        scene.idle(10);
        scene.world().setKineticSpeed(kinetics, 64);
        scene.overlay().showText(60)
                .text("Rotation goes into the drive")
                .pointAt(util.vector().blockSurface(util.grid().at(5, 1, 3), Direction.EAST))
                .placeNearTarget();
        scene.idle(70);
        scene.overlay().showText(60)
                .text("Breaking any part takes the whole mill apart again")
                .pointAt(util.vector().topOf(util.grid().at(5, 3, 3)))
                .placeNearTarget();
        scene.idle(70);
    }

    private static void doors(CreateSceneBuilder scene, SceneBuildingUtil util, boolean open) {
        scene.world().modifyBlockEntityNBT(util.select().position(1, 2, 2), CncBlockEntity.class, nbt -> nbt.putBoolean("DoorsOpen", open));
    }

    public static void machining(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("cnc_machining", "Machining with the CNC Mill");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        form(scene, util);
        BlockPos pendant = util.grid().at(1, 2, 2);
        BlockPos front = util.grid().at(3, 2, 2);
        Selection kinetics = util.select().fromTo(5, 1, 3, 6, 1, 3);
        scene.world().setKineticSpeed(kinetics, 64);
        scene.world().showSection(util.select().fromTo(1, 0, 2, 6, 4, 4), Direction.DOWN);
        scene.idle(15);

        scene.overlay().showControls(util.vector().blockSurface(pendant, Direction.NORTH), Pointing.DOWN, 30).rightClick();
        scene.idle(10);
        doors(scene, util, true);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("Right click the pendant to open the doors. It won't cut while they're open")
                .pointAt(util.vector().blockSurface(pendant, Direction.NORTH))
                .placeNearTarget();
        scene.idle(80);

        ItemStack sheet = AllItems.GOLDEN_SHEET.asStack();
        List<ItemStack> parts = List.of(sheet, new ItemStack(AllBlocks.COGWHEEL.get()), new ItemStack(AllBlocks.LARGE_COGWHEEL.get()), new ItemStack(Items.IRON_NUGGET));
        scene.overlay().showControls(util.vector().blockSurface(front, Direction.NORTH), Pointing.DOWN, 30).rightClick().withItem(sheet);
        scene.idle(10);
        contents(scene, util, parts, List.of(), false, ItemStack.EMPTY);
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("Load parts through the front, and take things out with an empty hand, a stack at a time")
                .pointAt(util.vector().blockSurface(front, Direction.NORTH))
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showControls(util.vector().blockSurface(pendant, Direction.NORTH), Pointing.DOWN, 30).rightClick();
        scene.idle(10);
        doors(scene, util, false);
        contents(scene, util, parts, List.of(), true, AllItems.PRECISION_MECHANISM.asStack());
        scene.overlay().showText(90)
                .attachKeyFrame()
                .text("Shut the doors and it gets to work. It makes anything a cnc or deploying recipe asks for, and a whole sequenced assembly in one go")
                .pointAt(util.vector().topOf(util.grid().at(3, 1, 3)))
                .placeNearTarget();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("The light on top is green while it cuts, amber when it has nothing to do and red without rotation")
                .pointAt(util.vector().topOf(util.grid().at(2, 3, 2)).add(0, .7, 0))
                .placeNearTarget();
        scene.idle(90);

        BlockPos inFunnel = util.grid().at(4, 4, 3);
        BlockPos outFunnel = util.grid().at(1, 1, 3);
        scene.world().setBlock(inFunnel, AllBlocks.ANDESITE_FUNNEL.getDefaultState().setValue(FunnelBlock.FACING, Direction.UP), false);
        scene.world().showSection(util.select().position(inFunnel), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80)
                .attachKeyFrame()
                .colored(PonderPalette.INPUT)
                .text("Automation feeds it through the hatch on the roof, and nowhere else")
                .pointAt(util.vector().topOf(inFunnel))
                .placeNearTarget();
        scene.idle(90);

        scene.world().setBlock(outFunnel, AllBlocks.ANDESITE_FUNNEL.getDefaultState().setValue(FunnelBlock.FACING, Direction.WEST), false);
        scene.world().showSection(util.select().position(outFunnel), Direction.EAST);
        scene.idle(10);
        contents(scene, util, List.of(), List.of(AllItems.PRECISION_MECHANISM.asStack()), false, ItemStack.EMPTY);
        scene.overlay().showText(80)
                .attachKeyFrame()
                .colored(PonderPalette.OUTPUT)
                .text("Finished pieces only come out of the hatch on the pendant's end. Funnels, chutes and arms all work on the two hatches")
                .pointAt(util.vector().blockSurface(util.grid().at(2, 1, 3), Direction.WEST))
                .placeNearTarget();
        scene.idle(90);
    }
}
