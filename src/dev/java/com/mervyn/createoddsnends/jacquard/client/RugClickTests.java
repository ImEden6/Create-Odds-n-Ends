package com.mervyn.createoddsnends.jacquard.client;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.mervyn.createoddsnends.Tracer;
import com.mervyn.createoddsnends.jacquard.Design;
import com.mervyn.createoddsnends.jacquard.DesignGen;
import com.mervyn.createoddsnends.jacquard.Designs;
import com.mervyn.createoddsnends.jacquard.TapestryEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.client.event.ClientTickEvent;

// Dev only, ODDSNENDS_RUGTEST=true with the "rugs" showcase set. Stands the player in front of a rug
// up in the air, aims at it, fires the real right and left click code and checks what the server did.
@EventBusSubscriber(modid = Tracer.MODID, value = Dist.CLIENT)
public final class RugClickTests {
    static final boolean ENABLED = "true".equals(System.getenv("ODDSNENDS_RUGTEST"));
    private static final int START = 200, STEP = 60;
    private static final Method USE = ObfuscationReflectionHelper.findMethod(Minecraft.class, "startUseItem");
    private static final Method ATTACK = ObfuscationReflectionHelper.findMethod(Minecraft.class, "startAttack");

    private record Case(String name, GameType mode, ItemStack main, ItemStack off, boolean use, Block expect, boolean rugGone) {}

    private static final List<Case> CASES = List.of(
            new Case("place_held_block_on_rug", GameType.SURVIVAL, new ItemStack(Items.STONE), ItemStack.EMPTY, true, Blocks.STONE, false),
            new Case("place_offhand_torch_on_rug", GameType.SURVIVAL, new ItemStack(Items.IRON_SWORD), new ItemStack(Items.TORCH), true, Blocks.TORCH, false),
            new Case("sword_alone_does_nothing", GameType.SURVIVAL, new ItemStack(Items.IRON_SWORD), ItemStack.EMPTY, true, Blocks.AIR, false),
            new Case("offhand_torch_still_breaks_rug", GameType.SURVIVAL, new ItemStack(Items.IRON_SWORD), new ItemStack(Items.TORCH), false, Blocks.AIR, true),
            new Case("creative_block_breaks_rug_not_floor", GameType.CREATIVE, new ItemStack(Items.STONE), ItemStack.EMPTY, false, Blocks.AIR, true));

    private static int tick;
    private static BlockPos base;
    private static final List<String> failures = new ArrayList<>();

    private RugClickTests() {}

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        IntegratedServer server = mc.getSingleplayerServer();
        if (!ENABLED || mc.player == null || mc.level == null || server == null)
            return;
        tick++;
        if (tick == START)
            base = mc.player.blockPosition().above(24);
        if (base == null)
            return;
        for (int i = 0; i < CASES.size(); i++) {
            int at = START + i * STEP, n = i;
            if (tick == at)
                server.execute(() -> setup(player(server), n));
            if (tick == at + 25)
                click(mc, n);
            if (tick == at + 50)
                server.execute(() -> verify(player(server).serverLevel(), n));
        }
        if (tick == START + CASES.size() * STEP + 10)
            Tracer.LOGGER.info("RUGTEST CLICKS DONE {} failures {}", failures.size(), failures);
    }

    private static ServerPlayer player(IntegratedServer server) {
        return server.getPlayerList().getPlayers().get(0);
    }

    private static BlockPos spot(int i) {
        return base.relative(Direction.EAST, i * 4);
    }

    private static Vec3 rugCentre(int i) {
        return Vec3.atBottomCenterOf(spot(i)).add(0, 0.03, 0);
    }

    private static void check(String name, boolean ok, String detail) {
        Tracer.LOGGER.info("RUGTEST {} {} {}", ok ? "PASS" : "FAIL", name, detail);
        if (!ok)
            failures.add(name);
    }

    private static void setup(ServerPlayer player, int i) {
        ServerLevel level = player.serverLevel();
        BlockPos spot = spot(i);
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 3; z++) {
                level.setBlock(spot.offset(x, -1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
                for (int y = 0; y < 3; y++)
                    level.setBlock(spot.offset(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        Design design = DesignGen.generate(9, 1, 1, 4, "kilim");
        level.addFreshEntity(new TapestryEntity(level, spot, Direction.UP, Direction.NORTH, Designs.get(level.getServer()).add(design), design));
        Case c = CASES.get(i);
        player.setGameMode(c.mode());
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        player.setItemSlot(EquipmentSlot.MAINHAND, c.main().copy());
        player.setItemSlot(EquipmentSlot.OFFHAND, c.off().copy());
        Vec3 feet = Vec3.atBottomCenterOf(spot.south(2));
        player.teleportTo(level, feet.x, feet.y, feet.z, 180, 50);
    }

    private static void click(Minecraft mc, int i) {
        Case c = CASES.get(i);
        mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, rugCentre(i));
        mc.gameRenderer.pick(1);
        check(c.name() + "_aims_at_rug", onRug(mc.hitResult), String.valueOf(mc.hitResult.getType()));
        try {
            (c.use() ? USE : ATTACK).invoke(mc);
        } catch (ReflectiveOperationException e) {
            check(c.name() + "_click", false, e.toString());
        }
        check(c.name() + "_still_aims_at_rug_after", onRug(mc.hitResult), String.valueOf(mc.hitResult.getType()));
    }

    private static boolean onRug(HitResult hit) {
        return hit instanceof EntityHitResult e && e.getEntity() instanceof TapestryEntity;
    }

    private static void verify(ServerLevel level, int i) {
        Case c = CASES.get(i);
        BlockPos spot = spot(i);
        boolean rug = !level.getEntitiesOfClass(TapestryEntity.class, new AABB(spot)).isEmpty();
        check(c.name(), level.getBlockState(spot).is(c.expect()) && rug != c.rugGone() && level.getBlockState(spot.below()).is(Blocks.STONE),
                "at " + level.getBlockState(spot) + " floor " + level.getBlockState(spot.below()) + " rug " + rug);
    }
}
