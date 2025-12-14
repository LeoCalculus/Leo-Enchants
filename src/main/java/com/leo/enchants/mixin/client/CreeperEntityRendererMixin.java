package com.leo.enchants.mixin.client;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to rotate creepers like missiles when in missile mode.
 * The entire creeper body tilts to point toward the target.
 */
@Mixin(LivingEntityRenderer.class)
public class CreeperEntityRendererMixin {
    
    /**
     * Inject into setupTransforms to apply missile body rotation for creepers.
     * Rotates the creeper so its HEAD points toward the target (like a missile).
     */
    @Inject(method = "setupTransforms", at = @At("TAIL"))
    private void applyMissileBodyRotation(LivingEntityRenderState state, MatrixStack matrices, float animationProgress, float bodyYaw, CallbackInfo ci) {
        // Only apply to creepers in missile mode
        if (state instanceof MissileCreeperRenderStateAccessor accessor) {
            if (accessor.leo_enchants$isMissileMode()) {
                float pitch = accessor.leo_enchants$getMissilePitch();
                
                // Rotate -90 degrees so the creeper's HEAD points forward (not feet)
                // - pitch = 0 (level target) -> creeper tilts -90° (head forward)
                // - pitch < 0 (target above) -> head points up
                // - pitch > 0 (target below) -> head points down
                float missileRotation = -90.0f - pitch;
                
                // Pivot around center of creeper
                matrices.translate(0, 0.85f, 0);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(missileRotation));
                matrices.translate(0, -0.85f, 0);
            }
        }
    }
}
