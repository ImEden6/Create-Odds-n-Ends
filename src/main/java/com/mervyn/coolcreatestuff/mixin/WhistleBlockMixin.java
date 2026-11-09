package com.mervyn.coolcreatestuff.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mervyn.coolcreatestuff.organ.Organ;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

// Steam whistles only stand on fluid tanks; this lets them stand on a wind chest too.
@Mixin(WhistleBlock.class)
public class WhistleBlockMixin {
    @Inject(method = "canSurvive", at = @At("RETURN"), cancellable = true)
    private void coolstuffs$onWindChest(BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && level.getBlockState(pos.relative(WhistleBlock.getAttachedDirection(state))).is(Organ.WIND_CHEST.get()))
            cir.setReturnValue(true);
    }
}
