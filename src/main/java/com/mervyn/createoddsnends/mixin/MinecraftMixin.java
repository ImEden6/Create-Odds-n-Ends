package com.mervyn.createoddsnends.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mervyn.createoddsnends.jacquard.TapestryEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Unique
    private HitResult oddsnends$rug;

    @Inject(method = "startUseItem", at = @At("HEAD"))
    private void oddsnends$useThroughRug(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        LocalPlayer player = mc.player;
        if (player == null || !(mc.hitResult instanceof EntityHitResult hit) || !(hit.getEntity() instanceof TapestryEntity rug) || rug.getDirection() != Direction.UP)
            return;
        if (!(player.getMainHandItem().getItem() instanceof BlockItem) && !(player.getOffhandItem().getItem() instanceof BlockItem))
            return;
        oddsnends$rug = mc.hitResult;
        mc.hitResult = player.pick(player.blockInteractionRange(), 1, false);
    }

    @Inject(method = "startUseItem", at = @At("RETURN"))
    private void oddsnends$backOnRug(CallbackInfo ci) {
        if (oddsnends$rug != null) {
            ((Minecraft) (Object) this).hitResult = oddsnends$rug;
            oddsnends$rug = null;
        }
    }
}
