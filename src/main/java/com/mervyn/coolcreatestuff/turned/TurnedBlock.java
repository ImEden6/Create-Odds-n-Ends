package com.mervyn.coolcreatestuff.turned;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.mervyn.coolcreatestuff.Profile;
import com.mervyn.coolcreatestuff.Tracer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import net.neoforged.neoforge.event.EventHooks;

public class TurnedBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    // sources whose cut ledges show their end texture (rings) rather than a slice of the side;
    // anything with an axis property, like logs and pillars, already counts
    public static final TagKey<Block> END_GRAIN = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Tracer.MODID, "end_grain"));

    private record ShapeKey(Profile profile, Direction.Axis axis) {}

    private static final Map<ShapeKey, VoxelShape> SHAPES = new ConcurrentHashMap<>();

    public TurnedBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.Y));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return RotatedPillarBlock.rotatePillar(state, rotation);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            AuxiliaryLightManager lights = level.getAuxLightManager(pos);
            if (lights != null)
                lights.removeLightAt(pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TurnedBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof TurnedBlockEntity be)
            return shapeOf(be.getProfile(), state.getValue(AXIS));
        return Shapes.block();
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        if (level.getBlockEntity(pos) instanceof TurnedBlockEntity be)
            stack.applyComponents(be.collectComponents());
        return stack;
    }

    static BlockState source(BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof TurnedBlockEntity be && !(be.getSource() instanceof TurnedBlock))
            return be.getSource().defaultBlockState();
        return Blocks.OAK_PLANKS.defaultBlockState();
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return source(level, pos).getDestroyProgress(player, level, pos);
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        return EventHooks.doPlayerHarvestCheck(player, source(level, pos), level, pos);
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return source(level, pos).getSoundType(level, pos, entity);
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return source(level, pos).getBlock().getExplosionResistance();
    }

    @Override
    public MapColor getMapColor(BlockState state, BlockGetter level, BlockPos pos, MapColor defaultColor) {
        return source(level, pos).getMapColor(level, pos);
    }

    // the light engine can't read block entities, so the BE parks its source's light here
    @Override
    public boolean hasDynamicLightEmission(BlockState state) {
        return true;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        AuxiliaryLightManager lights = level.getAuxLightManager(pos);
        return lights != null ? lights.getLightAt(pos) : 0;
    }

    public static VoxelShape shapeOf(Profile profile, Direction.Axis axis) {
        return SHAPES.computeIfAbsent(new ShapeKey(profile, axis), k -> {
            VoxelShape shape = Shapes.empty();
            int start = 0;
            for (int i = 1; i <= Profile.LAYERS; i++) {
                if (i < Profile.LAYERS && profile.radius(i) == profile.radius(start))
                    continue;
                shape = Shapes.or(shape, layer(profile.radius(start), start, i, axis));
                start = i;
            }
            return shape.optimize();
        });
    }

    // Matches the model rotations TurnedQuads bakes with: x lies foot west, z lies foot south.
    private static VoxelShape layer(int r, int from, int to, Direction.Axis axis) {
        return switch (axis) {
            case Y -> Block.box(8 - r, from, 8 - r, 8 + r, to, 8 + r);
            case X -> Block.box(from, 8 - r, 8 - r, to, 8 + r, 8 + r);
            case Z -> Block.box(8 - r, 8 - r, 16 - to, 8 + r, 8 + r, 16 - from);
        };
    }
}
