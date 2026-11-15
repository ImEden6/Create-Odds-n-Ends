package com.mervyn.createoddsnends.jacquard;

import java.util.ArrayList;
import java.util.List;

import com.mervyn.createoddsnends.Tracer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.WeightedPressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

// Dev only, ODDSNENDS_RUGTEST=true. Lays 1x1 rugs, stands a pressed plate of every kind on each and
// logs one RUGTEST line per check once the plates should have let go.
@EventBusSubscriber(modid = Tracer.MODID)
public final class RugTests {
    static final boolean ENABLED = "true".equals(System.getenv("ODDSNENDS_RUGTEST"));
    private static final int START = 100;
    private static final Block[] PLATES = {Blocks.OAK_PRESSURE_PLATE, Blocks.CRIMSON_PRESSURE_PLATE, Blocks.STONE_PRESSURE_PLATE,
            Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE};

    private static int tick;
    private static BlockPos base;
    private static final List<TapestryEntity> rugs = new ArrayList<>();
    private static final List<String> failures = new ArrayList<>();

    private RugTests() {}

    @SubscribeEvent
    static void tick(ServerTickEvent.Post event) {
        if (!ENABLED)
            return;
        MinecraftServer server = event.getServer();
        if (server.getPlayerList().getPlayers().isEmpty())
            return;
        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        ServerLevel level = player.serverLevel();
        tick++;
        if (tick == START) {
            base = player.blockPosition().relative(Direction.WEST, 30);
            setup(level);
        }
        if (tick == START + 60)
            plates(level);
        if (tick == START + 220)
            finish();
    }

    private static BlockPos spot(int i) {
        return base.relative(Direction.NORTH, i * 2);
    }

    private static void check(String name, boolean ok, String detail) {
        Tracer.LOGGER.info("RUGTEST {} {} {}", ok ? "PASS" : "FAIL", name, detail);
        if (!ok)
            failures.add(name);
    }

    private static void setup(ServerLevel level) {
        Design design = DesignGen.generate(5, 1, 1, 4, "kilim");
        int id = Designs.get(level.getServer()).add(design);
        for (int i = 0; i < PLATES.length; i++) {
            BlockPos pos = spot(i);
            level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            TapestryEntity rug = new TapestryEntity(level, pos, Direction.UP, Direction.NORTH, id, design);
            check("rug_laid_" + i, rug.survives(), pos.toShortString());
            level.addFreshEntity(rug);
            rugs.add(rug);
            BlockState plate = PLATES[i].defaultBlockState();
            plate = plate.hasProperty(PressurePlateBlock.POWERED) ? plate.setValue(PressurePlateBlock.POWERED, true) : plate.setValue(WeightedPressurePlateBlock.POWER, 15);
            level.setBlock(pos, plate, Block.UPDATE_ALL);
            level.scheduleTick(pos, PLATES[i], 1);
        }
    }

    private static void plates(ServerLevel level) {
        for (int i = 0; i < PLATES.length; i++) {
            BlockState state = level.getBlockState(spot(i));
            int signal = state.getBlock() instanceof BasePressurePlateBlock ? state.getSignal(level, spot(i), Direction.UP) : -1;
            check("plate_lets_go_" + PLATES[i].getDescriptionId().replace("block.minecraft.", ""), signal == 0, state.toString());
        }
    }

    private static void finish() {
        for (int i = 0; i < rugs.size(); i++)
            check("rug_stays_under_plate_" + i, !rugs.get(i).isRemoved(), rugs.get(i).blockPosition().toShortString());
        Tracer.LOGGER.info("RUGTEST DONE {} failures {}", failures.size(), failures);
    }
}
