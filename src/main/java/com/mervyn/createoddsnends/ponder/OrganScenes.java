package com.mervyn.createoddsnends.ponder;

import com.mervyn.createoddsnends.organ.BarrelOrganBlockEntity;
import com.mervyn.createoddsnends.organ.InstrumentBlock;
import com.mervyn.createoddsnends.organ.InstrumentBlockEntity;
import com.mervyn.createoddsnends.organ.Organ;
import com.mervyn.createoddsnends.organ.ScoreSync;
import com.mervyn.createoddsnends.organ.WindChestBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public final class OrganScenes {
    private static final BlockPos ORGAN = new BlockPos(2, 1, 2);
    private static final BlockPos NET_ORGAN = new BlockPos(3, 1, 5);

    private OrganScenes() {}

    private static ItemStack book() {
        ItemStack stack = new ItemStack(Organ.BOOK_MUSIC.get());
        stack.set(Organ.SCORE.get(), ScoreSync.PONDER);
        return stack;
    }

    private static void play(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos organ, boolean running, boolean finished) {
        scene.world().modifyBlockEntityNBT(util.select().position(organ), BarrelOrganBlockEntity.class, nbt -> {
            nbt.putBoolean("Running", running);
            nbt.putBoolean("Finished", finished);
            if (running)
                nbt.putFloat("Row", 0);
        });
    }

    private static void blow(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos chest, boolean on, boolean whistle) {
        scene.world().modifyBlockEntity(chest, WindChestBlockEntity.class, be -> be.showBlowing(on));
        if (whistle)
            scene.world().modifyBlock(chest.above(), s -> s.setValue(BlockStateProperties.POWERED, on), false);
    }

    public static void organ(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("organ_basics", "Playing Book Music");
        scene.configureBasePlate(0, 0, 5);
        Selection kinetics = util.select().fromTo(2, 1, 2, 2, 1, 4);
        scene.world().setKineticSpeed(kinetics, 0);
        scene.showBasePlate();
        scene.idle(10);

        scene.world().showSection(util.select().fromTo(2, 1, 2, 2, 2, 2), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("The barrel organ is two blocks tall. Its front faces you when you place it")
                .pointAt(util.vector().blockSurface(ORGAN, Direction.NORTH))
                .placeNearTarget();
        scene.idle(80);

        scene.world().showSection(util.select().fromTo(2, 1, 3, 2, 1, 4), Direction.NORTH);
        scene.idle(10);
        scene.overlay().showText(60)
                .text("Rotation goes into the back. Faster rotation plays faster")
                .pointAt(util.vector().blockSurface(ORGAN, Direction.SOUTH))
                .placeNearTarget();
        scene.idle(70);

        Vec3 front = util.vector().blockSurface(ORGAN, Direction.NORTH);
        ItemStack book = book();
        scene.overlay().showControls(front, Pointing.DOWN, 40).rightClick().withItem(book);
        scene.idle(7);
        Scenes.put(scene, util, ORGAN, BarrelOrganBlockEntity.class, "Book", book);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("Right-click it with book music from the card punch to load it. An empty hand takes it back out")
                .pointAt(front)
                .placeNearTarget();
        scene.idle(80);

        scene.world().setKineticSpeed(kinetics, 32);
        play(scene, util, ORGAN, true, false);
        scene.overlay().showText(80)
                .text("It plays the book from the start, turning the pages as it goes")
                .pointAt(util.vector().topOf(ORGAN.above()))
                .placeNearTarget();
        scene.idle(90);

        scene.world().showSection(util.select().position(3, 1, 2), Direction.WEST);
        scene.idle(10);
        play(scene, util, ORGAN, false, true);
        scene.world().toggleRedstonePower(util.select().position(3, 1, 2));
        scene.effects().indicateRedstone(ORGAN);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .colored(PonderPalette.RED)
                .text("On Play once it stops at the end and sends out a short redstone pulse")
                .pointAt(util.vector().centerOf(3, 1, 2))
                .placeNearTarget();
        scene.idle(20);
        scene.world().toggleRedstonePower(util.select().position(3, 1, 2));
        scene.idle(60);

        Vec3 side = util.vector().blockSurface(ORGAN, Direction.WEST);
        scene.overlay().showControls(side, Pointing.LEFT, 40).rightClick();
        scene.idle(7);
        scene.world().modifyBlockEntityNBT(util.select().position(ORGAN), BarrelOrganBlockEntity.class, nbt -> nbt.putInt("ScrollValue", 1));
        play(scene, util, ORGAN, true, false);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("The value box on its side switches it to Loop, and it starts over every time it reaches the end")
                .pointAt(side)
                .placeNearTarget();
        scene.idle(80);

        scene.world().showSection(util.select().position(3, 2, 2), Direction.WEST);
        scene.idle(10);
        scene.world().toggleRedstonePower(util.select().position(3, 2, 2));
        scene.effects().indicateRedstone(new BlockPos(3, 2, 2));
        play(scene, util, ORGAN, false, false);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("A redstone signal on either half pauses it. Turn it off and it carries on from the same spot")
                .pointAt(util.vector().centerOf(3, 2, 2))
                .placeNearTarget();
        scene.idle(80);
        scene.world().toggleRedstonePower(util.select().position(3, 2, 2));
        scene.world().modifyBlockEntityNBT(util.select().position(ORGAN), BarrelOrganBlockEntity.class, nbt -> nbt.putBoolean("Running", true));
        scene.idle(30);
    }

    public static void windNetwork(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("wind_network", "Wind Chests, Whistles and Instruments");
        scene.configureBasePlate(0, 0, 7);
        Selection kinetics = util.select().fromTo(3, 1, 5, 3, 1, 6);
        scene.world().setKineticSpeed(kinetics, 0);
        BlockPos small = new BlockPos(1, 1, 2), medium = new BlockPos(3, 1, 2), large = new BlockPos(5, 1, 2);
        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(3, 1, 5, 3, 2, 6), Direction.DOWN);
        Scenes.put(scene, util, NET_ORGAN, BarrelOrganBlockEntity.class, "Book", book());
        scene.idle(15);

        scene.world().showSection(util.select().fromTo(1, 1, 3, 5, 1, 4), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("Wind lines carry the music from the organ. They join up with each other on their own")
                .pointAt(util.vector().centerOf(3, 1, 4))
                .placeNearTarget();
        scene.idle(80);

        scene.world().showSection(util.select().fromTo(1, 1, 2, 5, 1, 2), Direction.SOUTH);
        scene.idle(10);
        scene.overlay().showText(70)
                .text("Wind chests go anywhere on the lines. Every chest on the network hears every note")
                .pointAt(util.vector().topOf(medium))
                .placeNearTarget();
        scene.idle(80);

        scene.world().showSection(util.select().fromTo(1, 2, 2, 5, 4, 2), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("Put a steam whistle on a chest. It blows whenever the organ plays that whistle's note, no boiler needed")
                .pointAt(util.vector().topOf(medium.above()))
                .placeNearTarget();
        scene.idle(30);

        scene.world().setKineticSpeed(kinetics, 32);
        play(scene, util, NET_ORGAN, true, false);
        for (int i = 0; i < 3; i++) {
            BlockPos chest = i == 0 ? small : i == 1 ? medium : large;
            blow(scene, util, chest, true, true);
            scene.idle(12);
            blow(scene, util, chest, false, true);
            scene.idle(6);
        }
        scene.idle(10);

        scene.overlay().showText(80)
                .text("Whistle size and extensions set the pitch, the same way they always do")
                .pointAt(util.vector().topOf(large.above()))
                .placeNearTarget();
        scene.idle(90);

        Vec3 filter = util.vector().blockSurface(small, Direction.NORTH);
        scene.overlay().showControls(filter, Pointing.DOWN, 40).withItem(new ItemStack(Items.RED_DYE));
        scene.idle(7);
        scene.world().setFilterData(util.select().position(small), WindChestBlockEntity.class, new ItemStack(Items.RED_DYE));
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("Put a dye in the filter on the front to make a chest listen to one voice of the score only")
                .pointAt(filter)
                .placeNearTarget();
        scene.idle(90);

        Vec3 back = util.vector().blockSurface(large, Direction.SOUTH);
        scene.overlay().showControls(back, Pointing.DOWN, 40).rightClick();
        scene.idle(7);
        scene.world().modifyBlockEntityNBT(util.select().position(large), WindChestBlockEntity.class, nbt -> nbt.putInt("ScrollValue", 1));
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("The value box on the back picks Exact pitch, or Any octave so one whistle covers that note all the way up and down")
                .pointAt(back)
                .placeNearTarget();
        scene.idle(30);
        for (int i = 0; i < 3; i++) {
            blow(scene, util, large, true, true);
            scene.idle(8);
            blow(scene, util, large, false, true);
            scene.idle(8);
        }
        scene.idle(30);

        Selection tops = util.select().fromTo(1, 2, 2, 5, 4, 2);
        scene.world().hideSection(tops, Direction.UP);
        scene.idle(15);
        scene.world().setBlocks(tops, Blocks.AIR.defaultBlockState(), false);
        mount(scene, small, Organ.KICK_DRUM.get());
        mount(scene, medium, Organ.REED_PIPE.get());
        mount(scene, large, Organ.REED_BOX.get());
        scene.world().showSection(util.select().fromTo(1, 2, 2, 5, 2, 2), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(90)
                .attachKeyFrame()
                .text("Pipes and the reed box go on chests too. They play every note the filter lets through, at its real pitch, up to 6 at once")
                .pointAt(util.vector().topOf(medium.above()))
                .placeNearTarget();
        for (int i = 0; i < 4; i++) {
            strike(scene, medium.above(), 8);
            strike(scene, large.above(), 8);
            scene.idle(10);
            strike(scene, small.above(), 0);
            scene.idle(10);
        }
        scene.idle(20);

        scene.world().hideSection(util.select().position(small.above()), Direction.UP);
        scene.world().hideSection(util.select().position(large.above()), Direction.UP);
        scene.idle(15);
        mount(scene, small, Organ.SNARE_DRUM.get());
        mount(scene, large, Organ.WOOD_PIPE.get());
        scene.world().showSection(util.select().position(small.above()), Direction.DOWN);
        scene.world().showSection(util.select().position(large.above()), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(90)
                .attachKeyFrame()
                .text("Kick and snare drums only play their own lane of the score, whatever the filter says")
                .pointAt(util.vector().topOf(small.above()))
                .placeNearTarget();
        for (int i = 0; i < 4; i++) {
            strike(scene, small.above(), 0);
            strike(scene, large.above(), 12);
            scene.idle(20);
        }
        scene.idle(20);
    }

    private static void mount(CreateSceneBuilder scene, BlockPos chest, Block instrument) {
        scene.world().setBlock(chest.above(), instrument.defaultBlockState().setValue(InstrumentBlock.FACING, Direction.SOUTH), false);
    }

    private static void strike(CreateSceneBuilder scene, BlockPos pos, int ticks) {
        scene.world().modifyBlockEntity(pos, InstrumentBlockEntity.class, be -> be.strike(ticks));
    }

    public static void sequencer(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("wind_sequencer", "Wind Chests as a Sequencer");
        scene.configureBasePlate(0, 0, 7);
        Selection kinetics = util.select().fromTo(3, 1, 5, 3, 1, 6);
        scene.world().setKineticSpeed(kinetics, 0);
        BlockPos[] chests = {new BlockPos(0, 1, 2), new BlockPos(2, 1, 2), new BlockPos(4, 1, 2), new BlockPos(6, 1, 2)};
        ItemStack[] dyes = {new ItemStack(Items.RED_DYE), new ItemStack(Items.YELLOW_DYE), new ItemStack(Items.LIME_DYE), new ItemStack(Items.LIGHT_BLUE_DYE)};
        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(3, 1, 5, 3, 2, 6), Direction.DOWN);
        Scenes.put(scene, util, NET_ORGAN, BarrelOrganBlockEntity.class, "Book", book());
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(0, 1, 2, 6, 1, 4), Direction.DOWN);
        scene.idle(10);
        for (int i = 0; i < chests.length; i++)
            scene.world().setFilterData(util.select().position(chests[i]), WindChestBlockEntity.class, dyes[i]);

        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("A wind chest with no whistle on top still blows on any note it hears, as long as it matches the filter")
                .pointAt(util.vector().topOf(chests[1]))
                .placeNearTarget();
        scene.idle(90);

        scene.world().showSection(util.select().fromTo(0, 1, 1, 6, 1, 1), Direction.SOUTH);
        scene.idle(10);
        scene.overlay().showText(80)
                .text("While it blows it gives off a redstone signal from its sides, so it can drive lamps, doors or anything else")
                .pointAt(util.vector().centerOf(0, 1, 1))
                .placeNearTarget();
        scene.idle(90);

        scene.world().setKineticSpeed(kinetics, 32);
        play(scene, util, NET_ORGAN, true, false);
        scene.overlay().showText(100)
                .attachKeyFrame()
                .text("Give each chest a different voice and the score plays your redstone like a sequencer")
                .pointAt(util.vector().topOf(chests[2]))
                .placeNearTarget();
        for (int round = 0; round < 3; round++)
            for (int i = 0; i < chests.length; i++) {
                BlockPos lamp = chests[i].north();
                blow(scene, util, chests[i], true, false);
                scene.world().toggleRedstonePower(util.select().position(lamp));
                scene.idle(8);
                blow(scene, util, chests[i], false, false);
                scene.world().toggleRedstonePower(util.select().position(lamp));
            }
        scene.idle(20);
    }
}
