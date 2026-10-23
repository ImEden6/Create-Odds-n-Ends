package com.mervyn.createoddsnends.organ;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;

final class WindNetwork {
    static final int MAX_BLOCKS = 256, MAX_CHESTS = 64;
    private static final Map<Level, Set<BarrelOrganBlockEntity>> ORGANS = new WeakHashMap<>();

    private WindNetwork() {}

    static void track(BarrelOrganBlockEntity organ) {
        ORGANS.computeIfAbsent(organ.getLevel(), k -> Collections.newSetFromMap(new WeakHashMap<>())).add(organ);
    }

    static void untrack(BarrelOrganBlockEntity organ) {
        Set<BarrelOrganBlockEntity> organs = ORGANS.get(organ.getLevel());
        if (organs != null)
            organs.remove(organ);
    }

    // something on some network in this level changed, every organ there walks its lines again
    // once, next server tick
    static void changed(Level level) {
        if (level.isClientSide)
            return;
        Set<BarrelOrganBlockEntity> organs = ORGANS.get(level);
        if (organs != null)
            organs.forEach(BarrelOrganBlockEntity::queueRebuild);
    }

    private static boolean lineOpenTowards(BlockState state, Direction side) {
        return state.is(Organ.WIND_LINE.get()) && state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side));
    }

    static List<BlockPos> walk(Level level, BlockPos organ) {
        List<BlockPos> chests = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> todo = new ArrayDeque<>();
        seen.add(organ);
        for (Direction d : Direction.values())
            reach(level, organ.relative(d), d.getOpposite(), seen, todo);
        while (!todo.isEmpty()) {
            BlockPos pos = todo.pop();
            BlockState state = level.getBlockState(pos);
            boolean chest = state.is(Organ.WIND_CHEST.get());
            if (chest && chests.size() < MAX_CHESTS)
                chests.add(pos);
            for (Direction d : Direction.values())
                if (chest || lineOpenTowards(state, d))
                    reach(level, pos.relative(d), d.getOpposite(), seen, todo);
        }
        return chests;
    }

    private static void reach(Level level, BlockPos pos, Direction back, Set<BlockPos> seen, Deque<BlockPos> todo) {
        if (seen.size() > MAX_BLOCKS || seen.contains(pos) || !level.isLoaded(pos))
            return;
        BlockState state = level.getBlockState(pos);
        if (lineOpenTowards(state, back) || state.is(Organ.WIND_CHEST.get()) && lineOpenTowards(level.getBlockState(pos.relative(back)), back.getOpposite())) {
            seen.add(pos.immutable());
            todo.push(pos.immutable());
        }
    }
}
