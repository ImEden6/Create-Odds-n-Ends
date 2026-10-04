package com.mervyn.coolcreatestuff.jacquard;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// A woven design hung like a painting. Direction is the face it shows: horizontal on walls, UP as
// a rug, DOWN on a ceiling. Rugs and ceiling pieces also keep which way their top points.
public class TapestryEntity extends HangingEntity {
    private static final EntityDataAccessor<Integer> DESIGN = SynchedEntityData.defineId(TapestryEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WIDTH = SynchedEntityData.defineId(TapestryEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HEIGHT = SynchedEntityData.defineId(TapestryEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Direction> TOP = SynchedEntityData.defineId(TapestryEntity.class, EntityDataSerializers.DIRECTION);

    public TapestryEntity(EntityType<? extends TapestryEntity> type, Level level) {
        super(type, level);
    }

    public TapestryEntity(Level level, BlockPos pos, Direction face, Direction top, int designId, Design design) {
        super(Jacquard.TAPESTRY_ENTITY.get(), level, pos);
        entityData.set(DESIGN, designId);
        entityData.set(WIDTH, design.width());
        entityData.set(HEIGHT, design.height());
        entityData.set(TOP, top);
        setDirection(face);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DESIGN, -1);
        builder.define(WIDTH, 1);
        builder.define(HEIGHT, 1);
        builder.define(TOP, Direction.NORTH);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (key == WIDTH || key == HEIGHT || key == TOP)
            recalculateBoundingBox();
    }

    public int designId() {
        return entityData.get(DESIGN);
    }

    public int widthBlocks() {
        return entityData.get(WIDTH);
    }

    public int heightBlocks() {
        return entityData.get(HEIGHT);
    }

    public Direction top() {
        return entityData.get(TOP);
    }

    // like ItemFrame: floors and ceilings are allowed
    @Override
    protected void setDirection(Direction face) {
        this.direction = face;
        if (face.getAxis().isHorizontal()) {
            setXRot(0);
            setYRot(face.get2DDataValue() * 90);
        } else {
            setXRot(-90 * face.getAxisDirection().getStep());
            setYRot(0);
        }
        xRotO = getXRot();
        yRotO = getYRot();
        recalculateBoundingBox();
    }

    // Painting's box, generalised: width runs to the left of the face (or across the top
    // direction), height up the wall (or along the top direction)
    @Override
    protected AABB calculateBoundingBox(BlockPos pos, Direction face) {
        int w = entityData.get(WIDTH), h = entityData.get(HEIGHT);
        Vec3 centre = Vec3.atCenterOf(pos).relative(face, -0.46875);
        if (face.getAxis().isHorizontal()) {
            centre = centre.relative(face.getCounterClockWise(), offset(w)).relative(Direction.UP, offset(h));
            double x = face.getAxis() == Direction.Axis.X ? 0.0625 : w;
            double z = face.getAxis() == Direction.Axis.Z ? 0.0625 : w;
            return AABB.ofSize(centre, x, h, z);
        }
        Direction top = entityData.get(TOP);
        centre = centre.relative(top.getClockWise(), offset(w)).relative(top, offset(h));
        double x = top.getAxis() == Direction.Axis.X ? h : w;
        double z = top.getAxis() == Direction.Axis.Z ? h : w;
        return AABB.ofSize(centre, x, 0.0625, z);
    }

    private static double offset(int size) {
        return size % 2 == 0 ? 0.5 : 0;
    }

    public ItemStack asItem() {
        ItemStack stack = new ItemStack(Jacquard.TAPESTRY.get());
        stack.set(Jacquard.DESIGN.get(), designId());
        return stack;
    }

    @Override
    public void dropItem(@Nullable Entity brokenEntity) {
        if (!level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS))
            return;
        playSound(SoundEvents.PAINTING_BREAK, 1, 1);
        if (brokenEntity instanceof Player player && player.hasInfiniteMaterials())
            return;
        spawnAtLocation(asItem());
    }

    @Override
    public void playPlacementSound() {
        playSound(SoundEvents.PAINTING_PLACE, 1, 1);
    }

    @Override
    public ItemStack getPickResult() {
        return asItem();
    }

    @Override
    public void moveTo(double x, double y, double z, float yRot, float xRot) {
        setPos(x, y, z);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        setPos(x, y, z);
    }

    @Override
    public Vec3 trackingPosition() {
        return Vec3.atLowerCornerOf(pos);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Design", designId());
        tag.putInt("Width", widthBlocks());
        tag.putInt("Height", heightBlocks());
        tag.putByte("Facing", (byte) direction.get3DDataValue());
        tag.putByte("Top", (byte) top().get2DDataValue());
        super.addAdditionalSaveData(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(DESIGN, tag.getInt("Design"));
        entityData.set(WIDTH, Math.max(1, tag.getInt("Width")));
        entityData.set(HEIGHT, Math.max(1, tag.getInt("Height")));
        entityData.set(TOP, Direction.from2DDataValue(tag.getByte("Top")));
        direction = Direction.from3DDataValue(tag.getByte("Facing"));
        super.readAdditionalSaveData(tag);
        setDirection(direction);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity entity) {
        return new ClientboundAddEntityPacket(this, direction.get3DDataValue(), getPos());
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        setDirection(Direction.from3DDataValue(packet.getData()));
    }
}
