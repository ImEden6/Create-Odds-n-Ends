package com.mervyn.tracer.lathe;

import static com.mervyn.tracer.lathe.LathePartBlock.FORMED;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mervyn.tracer.Profile;
import com.mervyn.tracer.Tracer;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.sound.SoundScapes;
import com.simibubi.create.foundation.sound.SoundScapes.AmbienceGroup;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.ItemStackHandler;

public class HeadstockBlockEntity extends KineticBlockEntity {
    public record Work(Profile profile, Block source) {}

    final ItemStackHandler template = new Slot() {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(Tracer.PROFILE_TEMPLATE.get()) && stack.has(Tracer.PROFILE.get());
        }

        @Override
        protected void onContentsChanged(int slot) {
            progress = 0;
            super.onContentsChanged(slot);
        }
    };
    final ItemStackHandler input = new Slot() {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return Turnable.allows(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (getStackInSlot(0).isEmpty())
                progress = 0;
            super.onContentsChanged(slot);
        }
    };
    final ItemStackHandler output = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            changed();
        }
    };

    private class Slot extends ItemStackHandler {
        Slot() {
            super(1);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            changed();
        }
    }

    private float progress;
    private boolean cutting;
    private boolean tracer;

    public HeadstockBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // the headstock behind any formed lathe part, or null
    @Nullable
    public static HeadstockBlockEntity find(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(FORMED) || !state.getValue(FORMED))
            return null;
        BlockPos controller = LatheStructure.findController(level, pos, state);
        return controller != null && level.getBlockEntity(controller) instanceof HeadstockBlockEntity be ? be : null;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public float calculateStressApplied() {
        lastStressApplied = LatheWork.STRESS_IMPACT;
        return LatheWork.STRESS_IMPACT;
    }

    // covers the formed lathe, the tracer row behind it and the carriage hanging off the front, in any facing
    @Override
    protected AABB createRenderBoundingBox() {
        return new AABB(getBlockPos()).inflate(2, 1, 2);
    }

    private void changed() {
        if (level != null && !level.isClientSide)
            notifyUpdate();
    }

    // Both sides run this. The server owns progress and syncs whenever cutting starts or stops;
    // between those the client counts along on its own, the way Create's millstone does.
    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide) {
            if (cutting) {
                progress = Math.min(progress + Math.abs(getSpeed()), required());
                spawnChips();
            }
            return;
        }

        boolean can = canCut();
        if (can != cutting) {
            cutting = can;
            sendData();
        }
        if (!cutting)
            return;
        progress += Math.abs(getSpeed());
        if (progress >= required())
            finish();
    }

    private boolean canCut() {
        if (!getBlockState().getValue(FORMED) || getSpeed() == 0)
            return false;
        ItemStack in = input.getStackInSlot(0);
        if (templateProfile() == null || in.isEmpty() || !Turnable.allows(in))
            return false;
        ItemStack out = output.getStackInSlot(0);
        return out.isEmpty() || ItemStack.isSameItemSameComponents(out, result()) && out.getCount() < out.getMaxStackSize();
    }

    private ItemStack result() {
        ItemStack stack = new ItemStack(Tracer.TURNED_BLOCK_ITEM.get());
        stack.set(Tracer.PROFILE.get(), templateProfile());
        stack.set(Tracer.SOURCE_BLOCK.get(), inputBlock());
        return stack;
    }

    private void finish() {
        output.insertItem(0, result(), false);
        input.extractItem(0, 1, false);
        progress = 0;
        cutting = false;
        sendData();
    }

    private float required() {
        Block block = inputBlock();
        return block == null ? 1 : LatheWork.required(block);
    }

    @Nullable
    private Block inputBlock() {
        return input.getStackInSlot(0).getItem() instanceof BlockItem item ? item.getBlock() : null;
    }

    @Nullable
    public Profile templateProfile() {
        return template.getStackInSlot(0).get(Tracer.PROFILE.get());
    }

    public boolean isCutting() {
        return cutting;
    }

    // Carriage position along the piece in px: 16 at the tailstock end, 0 at the chuck.
    public float layerPos(float partialTick) {
        if (inputBlock() == null)
            return Profile.LAYERS;
        float done = progress + (cutting ? Math.abs(getSpeed()) * partialTick : 0);
        return Profile.LAYERS * (1 - Math.min(1, done / required()));
    }

    // What sits between the centres: the input with the layers behind the cutter turned down,
    // or a finished piece waiting in the output.
    @Nullable
    public Work work(float layerPos) {
        Block in = inputBlock();
        if (in != null) {
            Profile target = templateProfile();
            if (target == null)
                return new Work(Profile.FULL, in);
            int[] radii = new int[Profile.LAYERS];
            for (int i = 0; i < Profile.LAYERS; i++)
                radii[i] = i >= Mth.floor(layerPos) ? target.radius(i) : Profile.MAX_RADIUS;
            return new Work(Profile.of(radii), in);
        }
        ItemStack out = output.getStackInSlot(0);
        if (out.is(Tracer.TURNED_BLOCK_ITEM.get()))
            return new Work(out.getOrDefault(Tracer.PROFILE.get(), Profile.FULL), out.getOrDefault(Tracer.SOURCE_BLOCK.get(), Blocks.OAK_PLANKS));
        return null;
    }

    private void spawnChips() {
        Block in = inputBlock();
        Profile target = templateProfile();
        if (in == null || target == null)
            return;
        RandomSource random = level.random;
        if (random.nextInt(3) != 0)
            return;
        float layerPos = layerPos(0);
        int layer = Mth.clamp(Mth.floor(layerPos), 0, Profile.LAYERS - 1);
        Direction facing = getBlockState().getValue(HorizontalKineticBlock.HORIZONTAL_FACING);
        Direction front = facing.getClockWise();
        // model space to world: +x is the operator's side, -z runs toward the tailstock
        double x = (Profile.MAX_RADIUS + target.radius(layer)) / 16.0 - .5;
        double z = .5 + layerPos / 16;
        double px = worldPosition.getX() + .5 + front.getStepX() * x + facing.getStepX() * z;
        double py = worldPosition.getY() + .5;
        double pz = worldPosition.getZ() + .5 + front.getStepZ() * x + facing.getStepZ() * z;
        level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, in.defaultBlockState()), px, py, pz,
                front.getStepX() * .1 + random.nextGaussian() * .02, .1, front.getStepZ() * .1 + random.nextGaussian() * .02);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void tickAudio() {
        super.tickAudio();
        if (cutting && getSpeed() != 0)
            SoundScapes.play(AmbienceGroup.MILLING, worldPosition, Mth.clamp(Math.abs(getSpeed()) / 256f + .7f, .7f, 1.2f));
    }

    // Right click with a written template swaps it in, with an allowed block loads it.
    public ItemInteractionResult useItem(Player player, ItemStack held) {
        if (template.isItemValid(0, held)) {
            if (!level.isClientSide) {
                ItemStack old = template.getStackInSlot(0);
                template.setStackInSlot(0, held.consumeAndReturn(1, player));
                if (!old.isEmpty())
                    player.getInventory().placeItemBackInInventory(old);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (Turnable.allows(held)) {
            if (!input.getStackInSlot(0).isEmpty())
                return ItemInteractionResult.CONSUME;
            if (!level.isClientSide)
                input.setStackInSlot(0, held.consumeAndReturn(1, player));
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    // Empty hand takes the output, else the input; sneaking takes the template.
    public InteractionResult useEmptyHand(Player player) {
        ItemStackHandler from = player.isSecondaryUseActive() ? template
                : !output.getStackInSlot(0).isEmpty() ? output : input;
        if (from.getStackInSlot(0).isEmpty())
            return InteractionResult.PASS;
        if (!level.isClientSide)
            player.getInventory().placeItemBackInInventory(from.extractItem(0, 64, false));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void destroy() {
        super.destroy();
        ItemHelper.dropContents(level, worldPosition, template);
        ItemHelper.dropContents(level, worldPosition, input);
        ItemHelper.dropContents(level, worldPosition, output);
    }

    public boolean hasTracer() {
        return tracer;
    }

    void setTracer(boolean tracer) {
        if (this.tracer == tracer)
            return;
        this.tracer = tracer;
        notifyUpdate();
    }

    // /tracer lathe: drop a written template and one source block straight in
    public void debugLoad(Profile profile, Block source) {
        ItemStack written = new ItemStack(Tracer.PROFILE_TEMPLATE.get());
        written.set(Tracer.PROFILE.get(), profile);
        template.setStackInSlot(0, written);
        output.setStackInSlot(0, ItemStack.EMPTY);
        input.setStackInSlot(0, new ItemStack(source));
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        lang().translate("lathe.goggles.title").forGoggles(tooltip);

        Block in = inputBlock();
        int percent = in == null ? 0 : Mth.floor(100 * progress / required());
        lang().translate("lathe.goggles.progress", percent).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);

        Profile profile = templateProfile();
        lang().add(profile == null ? lang().translate("lathe.goggles.no_template") : lang().translate("lathe.goggles.template", profile.digits()))
                .style(ChatFormatting.GRAY).forGoggles(tooltip, 1);

        ItemStack out = output.getStackInSlot(0);
        if (in != null || !out.isEmpty()) {
            Component from = in != null ? in.getName() : Component.literal("-");
            Component to = !out.isEmpty() ? out.getHoverName() : Component.translatable("block.tracer.turned_block");
            lang().add(from.copy()).text(" → ").add(to.copy()).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
        }
        return true;
    }

    private static LangBuilder lang() {
        return new LangBuilder(Tracer.MODID);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Template", template.serializeNBT(registries));
        tag.put("Input", input.serializeNBT(registries));
        tag.put("Output", output.serializeNBT(registries));
        tag.putFloat("Progress", progress);
        tag.putBoolean("Cutting", cutting);
        tag.putBoolean("Tracer", tracer);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        template.deserializeNBT(registries, tag.getCompound("Template"));
        input.deserializeNBT(registries, tag.getCompound("Input"));
        output.deserializeNBT(registries, tag.getCompound("Output"));
        progress = tag.getFloat("Progress");
        cutting = tag.getBoolean("Cutting");
        tracer = tag.getBoolean("Tracer");
    }
}
