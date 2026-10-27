package com.mervyn.createoddsnends.jacquard.client;

import com.mervyn.createoddsnends.Tracer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

import com.mojang.serialization.MapCodec;

@EventBusSubscriber(modid = Tracer.MODID, bus = EventBusSubscriber.Bus.MOD)
final class CncMockup extends HorizontalDirectionalBlock {
    static final IntegerProperty FRAME = IntegerProperty.create("frame", 0, 3);
    static final IntegerProperty PART = IntegerProperty.create("part", 0, 59);
    static CncMockup block;

    private CncMockup() {
        super(BlockBehaviour.Properties.of().noOcclusion().strength(1));
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(FRAME, 0).setValue(PART, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(p -> new CncMockup());
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FRAME, PART);
    }

    @SubscribeEvent
    static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Tracer.MODID, "cnc_mockup"), () -> block = new CncMockup());
    }

    static void place(ServerLevel level, BlockPos origin, Direction facing, int frame) {
        for (int i = 0; i < 5; i++)
            for (int j = 0; j < 4; j++)
                for (int k = 0; k < 3; k++) {
                    BlockPos pos = origin.relative(facing.getClockWise(), i).above(j).relative(facing.getOpposite(), k);
                    level.setBlock(pos, block.defaultBlockState().setValue(FACING, facing).setValue(FRAME, frame).setValue(PART, i + 5 * (j + 4 * k)), 2);
                }
    }
}
