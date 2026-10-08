package com.mervyn.createoddsnends.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mervyn.createoddsnends.organ.WindChestBlock;
import com.mervyn.createoddsnends.organ.WindChestBlockEntity;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlockEntity;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;

// A whistle on a wind chest has no boiler, so the chest stands in for one on the client. Its steam
// comes out of the top instead, from the chest.
@Mixin(WhistleBlockEntity.class)
public class WhistleBlockEntityMixin {
    @Unique
    @Nullable
    private WindChestBlockEntity oddsnends$chest() {
        WhistleBlockEntity self = (WhistleBlockEntity) (Object) this;
        return WindChestBlock.whistleOnTop(self.getBlockState()) && self.getLevel().getBlockEntity(self.getBlockPos().below()) instanceof WindChestBlockEntity chest ? chest : null;
    }

    @ModifyVariable(method = "tick", at = @At("STORE"), ordinal = 0)
    private boolean oddsnends$blownByChest(boolean powered) {
        if (powered)
            return true;
        WindChestBlockEntity chest = oddsnends$chest();
        return chest != null && chest.isBlowing();
    }

    @Redirect(method = "tickAudio", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void oddsnends$noLowSteamOnChest(Level level, ParticleOptions particle, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        if (oddsnends$chest() == null)
            level.addParticle(particle, x, y, z, xSpeed, ySpeed, zSpeed);
    }
}
