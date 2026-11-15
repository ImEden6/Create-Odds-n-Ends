package com.mervyn.createoddsnends.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mervyn.createoddsnends.jacquard.TapestryEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.EntityHitResult;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow
    @Final
    Minecraft minecraft;

    @Inject(method = "pick(F)V", at = @At("TAIL"))
    private void oddsnends$throughRugs(float partialTicks, CallbackInfo ci) {
        Player player = minecraft.player;
        if (player == null || !(minecraft.hitResult instanceof EntityHitResult hit) || !(hit.getEntity() instanceof TapestryEntity rug) || rug.getDirection() != Direction.UP)
            return;
        if (!(player.getMainHandItem().getItem() instanceof BlockItem) && !(player.getOffhandItem().getItem() instanceof BlockItem))
            return;
        minecraft.hitResult = player.pick(player.blockInteractionRange(), partialTicks, false);
        minecraft.crosshairPickEntity = null;
    }
}
