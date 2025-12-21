package com.leo.enchants.mixin.client;

import com.leo.enchants.accessor.client.MagnifyRenderStateAccessor;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ProjectileEntityRenderer;
import net.minecraft.client.render.entity.state.ProjectileEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to scale magnified arrows during rendering in 1.21.8+.
 */
@Mixin(ProjectileEntityRenderer.class)
public class ProjectileEntityRendererMixin {
    
    @Unique
    private float leo_enchants$currentScale = 1.0f;
    
    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/ProjectileEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", 
            at = @At("HEAD"))
    private void applyMagnifyScale(ProjectileEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        leo_enchants$currentScale = 1.0f;
        
        // Check if the entity has magnify scale via the render state
        // We need to access the original entity through the state
        if (state instanceof MagnifyRenderStateAccessor accessor) {
            float scale = accessor.leo_enchants$getMagnifyScale();
            if (scale > 1.0f) {
                leo_enchants$currentScale = scale;
                matrices.push();
                matrices.scale(scale, scale, scale);
            }
        }
    }
    
    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/ProjectileEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", 
            at = @At("RETURN"))
    private void resetMagnifyScale(ProjectileEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (leo_enchants$currentScale > 1.0f) {
            matrices.pop();
        }
    }
}

