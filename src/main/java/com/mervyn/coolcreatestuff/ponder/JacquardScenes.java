package com.mervyn.coolcreatestuff.ponder;

import static com.mervyn.coolcreatestuff.jacquard.LoomPartBlock.FORMED;

import java.util.Arrays;

import com.mervyn.coolcreatestuff.jacquard.CardPunchBlockEntity;
import com.mervyn.coolcreatestuff.jacquard.DesignSync;
import com.mervyn.coolcreatestuff.jacquard.Jacquard;
import com.mervyn.coolcreatestuff.jacquard.JacquardHeadBlockEntity;
import com.mervyn.coolcreatestuff.jacquard.JacquardWork;
import com.simibubi.create.AllItems;
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
import net.minecraft.world.phys.Vec3;

public final class JacquardScenes {
    private static final BlockPos HEAD = new BlockPos(2, 3, 3);
    private static final BlockPos DRIVE = new BlockPos(3, 1, 2);
    private static final BlockPos PUNCH = new BlockPos(2, 1, 2);

    private JacquardScenes() {}

    public static void build(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("loom_build", "Building the Jacquard Loom");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        Selection motor = util.select().position(4, 1, 2);
        Selection kinetics = util.select().position(4, 1, 2).add(util.select().position(DRIVE));
        scene.world().setKineticSpeed(kinetics, 0);
        scene.idle(10);

        scene.world().showSection(util.select().fromTo(1, 1, 2, 3, 1, 3), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("The loom is three blocks wide, three tall and two deep, built from loom frames")
                .pointAt(util.vector().topOf(util.grid().at(1, 1, 2)))
                .placeNearTarget();
        scene.idle(80);

        scene.overlay().showText(70)
                .text("The loom drive goes in the bottom front corner on the left")
                .pointAt(util.vector().topOf(DRIVE))
                .placeNearTarget();
        scene.idle(80);

        scene.world().showSection(util.select().fromTo(1, 2, 2, 3, 2, 3), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showOutline(PonderPalette.RED, "hollow", util.select().fromTo(2, 2, 2, 2, 2, 3), 70);
        scene.overlay().showText(70)
                .colored(PonderPalette.RED)
                .text("Leave the middle of the middle layer empty, the warp runs through there")
                .pointAt(util.vector().centerOf(2, 2, 3))
                .placeNearTarget();
        scene.idle(80);

        scene.world().showSection(util.select().fromTo(1, 3, 2, 3, 3, 2).add(util.select().position(1, 3, 3)).add(util.select().position(3, 3, 3)), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(HEAD), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("The jacquard head goes on top, at the back in the middle. Its front is the loom's front")
                .pointAt(util.vector().topOf(HEAD))
                .placeNearTarget();
        scene.idle(80);

        scene.overlay().showControls(util.vector().topOf(HEAD), Pointing.DOWN, 30).rightClick().withItem(AllItems.WRENCH.asStack());
        scene.idle(10);
        Scenes.form(scene, util.select().fromTo(1, 1, 2, 3, 3, 3), FORMED, HEAD, DRIVE);
        scene.effects().indicateSuccess(HEAD);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .colored(PonderPalette.GREEN)
                .text("Wrench the head to put it together. If something is off, it tells you where")
                .pointAt(util.vector().topOf(HEAD))
                .placeNearTarget();
        scene.idle(80);

        scene.world().showSection(motor, Direction.WEST);
        scene.idle(10);
        scene.world().setKineticSpeed(kinetics, 32);
        scene.overlay().showText(70)
                .text("Rotation goes into the side of the drive")
                .pointAt(util.vector().blockSurface(DRIVE, Direction.EAST))
                .placeNearTarget();
        scene.idle(80);
        scene.overlay().showText(60)
                .text("Breaking any part takes the loom apart again")
                .pointAt(util.vector().topOf(util.grid().at(3, 3, 3)))
                .placeNearTarget();
        scene.idle(70);
    }

    public static void weaving(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("loom_weaving", "Weaving Tapestries");
        scene.configureBasePlate(0, 0, 5);
        Selection kinetics = util.select().position(4, 1, 2).add(util.select().position(DRIVE));
        Scenes.form(scene, util.select().fromTo(1, 1, 2, 3, 3, 3), FORMED, HEAD, DRIVE);
        scene.world().setKineticSpeed(kinetics, 0);
        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(15);

        Vec3 front = util.vector().blockSurface(util.grid().at(2, 1, 2), Direction.NORTH);
        ItemStack chain = designed(Jacquard.PUNCHED_CARD_CHAIN.get().getDefaultInstance());
        scene.overlay().showControls(front, Pointing.DOWN, 40).rightClick().withItem(chain);
        scene.idle(7);
        Scenes.put(scene, util, HEAD, JacquardHeadBlockEntity.class, "Chain", chain);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("Right-click any part of the loom with a punched card chain to load it")
                .pointAt(front)
                .placeNearTarget();
        scene.idle(80);

        scene.overlay().showControls(front, Pointing.DOWN, 40).rightClick().withItem(new ItemStack(Items.MAGENTA_WOOL, 16));
        scene.idle(7);
        int[] thread = new int[16];
        Arrays.fill(thread, JacquardWork.THREAD_CAP);
        scene.world().modifyBlockEntityNBT(util.select().position(HEAD), JacquardHeadBlockEntity.class, nbt -> nbt.putIntArray("Thread", thread));
        scene.overlay().showText(80)
                .text("Wool is its thread. Every wool block is 256 pixels of cloth, and it holds up to 64 of each colour")
                .pointAt(front)
                .placeNearTarget();
        scene.idle(90);

        scene.world().setKineticSpeed(kinetics, 64);
        scene.world().modifyBlockEntityNBT(util.select().position(HEAD), JacquardHeadBlockEntity.class, nbt -> {
            nbt.putFloat("Speed", 64);
            nbt.putBoolean("Weaving", true);
        });
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("Give it rotation and it weaves the design, one throw of the shuttle for each colour in a row")
                .pointAt(util.vector().centerOf(2, 2, 2))
                .placeNearTarget();
        scene.idle(100);
        scene.overlay().showText(80)
                .text("If a colour runs out it stops with the shed open and carries on once you add more")
                .pointAt(util.vector().topOf(HEAD))
                .placeNearTarget();
        scene.idle(110);

        scene.world().setKineticSpeed(kinetics, 0);
        scene.world().modifyBlockEntityNBT(util.select().position(HEAD), JacquardHeadBlockEntity.class, nbt -> {
            nbt.putFloat("Speed", 0);
            nbt.putBoolean("Weaving", false);
        });
        Scenes.put(scene, util, HEAD, JacquardHeadBlockEntity.class, "Output", designed(Jacquard.TAPESTRY.get().getDefaultInstance()));
        scene.overlay().showControls(front, Pointing.DOWN, 40).rightClick();
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("The finished tapestry waits in the loom and the chain stays for the next one. An empty hand takes the tapestry, sneak to take the chain")
                .pointAt(front)
                .placeNearTarget();
        scene.idle(90);
        scene.overlay().showText(70)
                .text("Funnels, hoppers and mechanical arms work on any part: chains and wool go in, tapestries come out")
                .pointAt(util.vector().topOf(util.grid().at(3, 3, 3)))
                .placeNearTarget();
        scene.idle(80);
    }

    public static void punch(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("card_punch", "Punching Card Chains");
        scene.configureBasePlate(0, 0, 5);
        Selection kinetics = util.select().position(1, 1, 2).add(util.select().position(PUNCH));
        scene.world().setKineticSpeed(kinetics, 0);
        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().position(PUNCH), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(1, 1, 2), Direction.EAST);
        scene.idle(15);

        Vec3 top = util.vector().topOf(PUNCH);
        scene.overlay().showControls(top, Pointing.DOWN, 40).rightClick();
        scene.idle(7);
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("Right-click the card punch to open its editor. Draw a design up to 3 blocks wide and 4 tall, then hit Save")
                .pointAt(top)
                .placeNearTarget();
        scene.idle(90);

        ItemStack blanks = new ItemStack(Jacquard.BLANK_CARD.get(), 32);
        scene.overlay().showControls(top, Pointing.DOWN, 40).withItem(blanks);
        scene.idle(7);
        Scenes.put(scene, util, PUNCH, CardPunchBlockEntity.class, "Blanks", blanks);
        scene.overlay().showText(80)
                .text("Every block of height in the design takes 16 blank cards. Put them in its blank slot, or let a funnel feed them in")
                .pointAt(top)
                .placeNearTarget();
        scene.idle(90);

        scene.world().setKineticSpeed(kinetics, 64);
        scene.world().modifyBlockEntityNBT(util.select().position(PUNCH), CardPunchBlockEntity.class, nbt -> {
            nbt.putInt("Job", DesignSync.PONDER);
            nbt.putInt("JobWidth", 3);
            nbt.putInt("JobHeight", 2);
            nbt.putFloat("Worked", 0);
            nbt.putBoolean("Punching", true);
        });
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("With rotation it punches the whole chain, card by card")
                .pointAt(top)
                .placeNearTarget();
        scene.idle(140);

        ItemStack chain = designed(Jacquard.PUNCHED_CARD_CHAIN.get().getDefaultInstance());
        chain.set(Jacquard.CARDS.get(), 32);
        scene.world().setKineticSpeed(kinetics, 0);
        Scenes.put(scene, util, PUNCH, CardPunchBlockEntity.class, "Blanks", ItemStack.EMPTY);
        Scenes.put(scene, util, PUNCH, CardPunchBlockEntity.class, "Output", chain);
        scene.world().modifyBlockEntityNBT(util.select().position(PUNCH), CardPunchBlockEntity.class, nbt -> {
            nbt.putFloat("Worked", 0);
            nbt.putBoolean("Punching", false);
        });
        scene.overlay().showText(80)
                .attachKeyFrame()
                .text("The chain waits in the output slot until you take it out of the menu, or a funnel pulls it")
                .pointAt(top)
                .placeNearTarget();
        scene.idle(90);
        scene.overlay().showText(90)
                .text("To copy a chain or a tapestry, put it in the copy slot instead of saving a design. The copy keeps the same design")
                .pointAt(top)
                .placeNearTarget();
        scene.idle(100);
    }

    private static ItemStack designed(ItemStack stack) {
        stack.set(Jacquard.DESIGN.get(), DesignSync.PONDER);
        return stack;
    }
}
