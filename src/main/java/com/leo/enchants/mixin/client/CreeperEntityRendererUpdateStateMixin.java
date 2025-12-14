package com.leo.enchants.mixin.client;

import com.leo.enchants.mixin.MissileCreeperAccessor;
import net.minecraft.client.render.entity.CreeperEntityRenderer;
import net.minecraft.client.render.entity.state.CreeperEntityRenderState;
import net.minecraft.entity.mob.CreeperEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to copy missile mode data from entity to render state.
 */
@Mixin(CreeperEntityRenderer.class)
public class CreeperEntityRendererUpdateStateMixin {
    
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/mob/CreeperEntity;Lnet/minecraft/client/render/entity/state/CreeperEntityRenderState;F)V",
            at = @At("TAIL"))
    private void copyMissileData(CreeperEntity creeper, CreeperEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (creeper instanceof MissileCreeperAccessor accessor && state instanceof MissileCreeperRenderStateAccessor stateAccessor) {
            stateAccessor.leo_enchants$setMissileMode(accessor.leo_enchants$isMissileMode());
            stateAccessor.leo_enchants$setMissilePitch(accessor.leo_enchants$getMissilePitch());
        }
    }
}
