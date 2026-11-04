package com.mervyn.createoddsnends.client;

import com.mervyn.createoddsnends.cnc.CncBlockEntity;
import com.mervyn.createoddsnends.cnc.CncDoors;
import com.mervyn.createoddsnends.cnc.CncFrameBlock;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CncDoorClicks {
    private CncDoorClicks() {}

    public static void onUse(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (!event.isUseItem() || player == null || mc.level == null || !player.getMainHandItem().isEmpty())
            return;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1);
        double reach = mc.hitResult != null && mc.hitResult.getType() != HitResult.Type.MISS
                ? eye.distanceTo(mc.hitResult.getLocation()) : player.blockInteractionRange();
        for (BlockPos pendant : CncDoors.open(mc.level)) {
            if (!(mc.level.getBlockEntity(pendant) instanceof CncBlockEntity cnc)
                    || !CncDoors.crossesDoor(pendant, cnc.getBlockState().getValue(CncFrameBlock.FACING), eye, look, reach))
                continue;
            PacketDistributor.sendToServer(new CncDoors.Close(pendant));
            event.setCanceled(true);
            event.setSwingHand(true);
            return;
        }
    }
}
