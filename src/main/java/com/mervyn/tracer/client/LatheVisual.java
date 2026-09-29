package com.mervyn.tracer.client;

import static com.mervyn.tracer.lathe.LathePartBlock.FORMED;
import static com.simibubi.create.content.kinetics.base.HorizontalKineticBlock.HORIZONTAL_FACING;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.joml.FrustumIntersection;
import org.joml.Quaternionf;

import com.mervyn.tracer.Profile;
import com.mervyn.tracer.lathe.HeadstockBlockEntity;
import com.mervyn.tracer.lathe.LatheStructure;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.BakedModelBuilder;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.model.baked.SinglePosVirtualBlockGetter;
import dev.engine_room.flywheel.lib.util.RendererReloadCache;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;

// Flywheel side of the formed lathe. Everything that spins is a RotatingInstance so the GPU turns
// it; only the carriage and stylus get moved each frame. Layout comes from LatheLayout, same as
// the fallback HeadstockRenderer.
public class LatheVisual extends KineticBlockEntityVisual<HeadstockBlockEntity> implements SimpleDynamicVisual, SimpleTickableVisual {
    private record Shape(Profile profile, BlockState source) {}

    private static final RendererReloadCache<Shape, Model> SHAPES = new RendererReloadCache<>(LatheVisual::buildShape);

    private final Direction facing;
    private final Quaternionf rotation;
    private final BlockPos slot;
    private final BlockPos tailstock;
    private final BlockPos master;
    private final PoseStack poseStack = new PoseStack();

    private final RotatingInstance shaft;
    private final RotatingInstance chuck;
    private final RotatingInstance liveCenter;
    private final TransformedInstance saddle;
    private final TransformedInstance crossSlide;
    private final TransformedInstance toolPost;
    private final TransformedInstance cutter;

    private Shape shape;
    private boolean tracer;
    private RotatingInstance workpiece;
    @Nullable
    private RotatingInstance masterPattern;
    @Nullable
    private TransformedInstance tracerSlide;
    @Nullable
    private TransformedInstance stylus;

    public static BlockEntityVisual<? super HeadstockBlockEntity> create(VisualizationContext ctx, HeadstockBlockEntity be, float partialTick) {
        BlockState state = be.getBlockState();
        if (state.getValue(FORMED))
            return new LatheVisual(ctx, be, partialTick);
        return new OrientedRotatingVisual<>(ctx, be, partialTick, Direction.SOUTH,
                state.getValue(HORIZONTAL_FACING).getOpposite(), Models.partial(AllPartialModels.SHAFT_HALF));
    }

    private LatheVisual(VisualizationContext ctx, HeadstockBlockEntity be, float partialTick) {
        super(ctx, be, partialTick);
        facing = blockState.getValue(HORIZONTAL_FACING);
        rotation = LatheLayout.facingRotation(facing);
        slot = pos.relative(facing);
        tailstock = pos.relative(facing, 2);
        master = slot.relative(LatheStructure.back(facing));

        shaft = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF))
                .createInstance()
                .rotateToFace(Direction.SOUTH, facing.getOpposite())
                .setup(be)
                .setPosition(getVisualPosition());
        shaft.setChanged();

        chuck = spinning(Models.partial(LathePartials.CHUCK), pos);
        liveCenter = spinning(Models.partial(LathePartials.LIVE_CENTER), tailstock);
        saddle = sliding(LathePartials.SADDLE);
        crossSlide = sliding(LathePartials.CROSS_SLIDE);
        toolPost = sliding(LathePartials.TOOL_POST);
        cutter = sliding(LathePartials.CUTTER);

        shape = shapeOf(be);
        tracer = be.hasTracer();
        buildWork();
        moveCarriage();
    }

    private static Model buildShape(Shape shape) {
        PoseStack ms = new PoseStack();
        LatheLayout.layOnAxis(ms);
        return new BakedModelBuilder(TurnedModel.of(shape.profile, shape.source))
                .level(SinglePosVirtualBlockGetter.createFullDark().blockState(shape.source))
                .poseStack(ms)
                .build();
    }

    private static Shape shapeOf(HeadstockBlockEntity be) {
        return new Shape(be.getProfile(), be.getSource().defaultBlockState());
    }

    // chuck, live centre and workpiece share one angle, so they skip Create's per-position offset
    private RotatingInstance spinning(Model model, BlockPos at) {
        RotatingInstance instance = instancerProvider().instancer(AllInstanceTypes.ROTATING, model).createInstance();
        instance.rotation.set(rotation);
        instance.setRotationAxis(facing.getAxis())
                .setRotationalSpeed(blockEntity.getSpeed() * RotatingInstance.SPEED_MULTIPLIER)
                .setRotationOffset(0)
                .setPosition(at.subtract(renderOrigin()))
                .setChanged();
        return instance;
    }

    private TransformedInstance sliding(PartialModel partial) {
        return instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(partial)).createInstance();
    }

    private void buildWork() {
        if (workpiece != null)
            workpiece.delete();
        deleteTracer();

        Model model = SHAPES.get(shape);
        workpiece = spinning(model, slot);
        if (tracer) {
            masterPattern = spinning(model, master);
            masterPattern.setRotationalSpeed(0).setChanged();
            tracerSlide = sliding(LathePartials.TRACER_SLIDE);
            stylus = sliding(LathePartials.STYLUS);
        }
        updateLight(0);
    }

    private void deleteTracer() {
        if (masterPattern != null)
            masterPattern.delete();
        if (tracerSlide != null)
            tracerSlide.delete();
        if (stylus != null)
            stylus.delete();
        masterPattern = null;
        tracerSlide = null;
        stylus = null;
    }

    @Override
    public void update(float partialTick) {
        shaft.setup(blockEntity).setChanged();
        float speed = blockEntity.getSpeed() * RotatingInstance.SPEED_MULTIPLIER;
        chuck.setRotationalSpeed(speed).setChanged();
        liveCenter.setRotationalSpeed(speed).setChanged();
        workpiece.setRotationalSpeed(speed).setChanged();

        Shape now = shapeOf(blockEntity);
        if (!now.equals(shape) || blockEntity.hasTracer() != tracer) {
            shape = now;
            tracer = blockEntity.hasTracer();
            buildWork();
        }
    }

    @Override
    public void beginFrame(DynamicVisual.Context ctx) {
        if (!isVisible(ctx.frustum()) || doDistanceLimitThisFrame(ctx))
            return;
        moveCarriage();
    }

    private void moveCarriage() {
        LatheLayout.Carriage carriage = LatheLayout.preview(shape.profile, AnimationTickHolder.getRenderTime(level));
        float along = -1 + carriage.along();
        place(saddle, 0, along);
        place(crossSlide, carriage.reach(), along);
        place(toolPost, carriage.reach(), along);
        place(cutter, carriage.reach(), along);
        if (tracer) {
            place(tracerSlide, -1, along);
            place(stylus, -1 - carriage.reach(), along);
        }
    }

    private void place(TransformedInstance instance, float x, float z) {
        poseStack.pushPose();
        BlockPos at = getVisualPosition();
        poseStack.translate(at.getX(), at.getY(), at.getZ());
        LatheLayout.toModelSpace(poseStack, facing);
        poseStack.translate(x, 0, z);
        instance.setTransform(poseStack).setChanged();
        poseStack.popPose();
    }

    @Override
    public void tick(TickableVisual.Context context) {
        applyOverstressEffect(blockEntity, shaft);
    }

    @Override
    public boolean isVisible(FrustumIntersection frustum) {
        BlockPos at = getVisualPosition().relative(facing);
        return frustum.testSphere(at.getX() + .5f, at.getY() + .5f, at.getZ() + .5f, 2.5f);
    }

    @Override
    public void setSectionCollector(SectionCollector collector) {
        lightSections = collector;
        LongSet sections = new LongOpenHashSet();
        for (BlockPos at : new BlockPos[] {pos, slot, tailstock, master})
            sections.add(SectionPos.asLong(at));
        collector.sections(sections);
    }

    @Override
    public void updateLight(float partialTick) {
        relight(pos, shaft, chuck);
        relight(tailstock, liveCenter);
        relight(slot, workpiece, saddle, crossSlide, toolPost, cutter);
        relight(master, masterPattern, tracerSlide, stylus);
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
        consumer.accept(shaft);
        consumer.accept(chuck);
    }

    @Override
    protected void _delete() {
        shaft.delete();
        chuck.delete();
        liveCenter.delete();
        saddle.delete();
        crossSlide.delete();
        toolPost.delete();
        cutter.delete();
        workpiece.delete();
        deleteTracer();
    }
}
