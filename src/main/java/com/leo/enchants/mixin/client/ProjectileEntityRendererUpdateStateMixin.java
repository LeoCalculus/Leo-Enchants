package com.leo.enchants.mixin.client;

import com.leo.enchants.accessor.MagnifyArrowAccessor;
import com.leo.enchants.accessor.client.MagnifyRenderStateAccessor;
import net.minecraft.client.render.entity.ProjectileEntityRenderer;
import net.minecraft.client.render.entity.state.ProjectileEntityRenderState;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to copy magnify scale from entity to render state.
 */
@Mixin(ProjectileEntityRenderer.class)
public class ProjectileEntityRendererUpdateStateMixin {
    
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/projectile/PersistentProjectileEntity;Lnet/minecraft/client/render/entity/state/ProjectileEntityRenderState;F)V", 
            at = @At("TAIL"))
    private void copyMagnifyScale(PersistentProjectileEntity entity, ProjectileEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (entity instanceof MagnifyArrowAccessor entityAccessor && state instanceof MagnifyRenderStateAccessor stateAccessor) {
            stateAccessor.leo_enchants$setMagnifyScale(entityAccessor.leo_enchants$getMagnifyScale());
        }
    }
}

