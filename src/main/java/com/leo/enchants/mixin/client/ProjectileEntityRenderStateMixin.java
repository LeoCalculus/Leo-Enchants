package com.leo.enchants.mixin.client;

import com.leo.enchants.accessor.client.MagnifyRenderStateAccessor;
import net.minecraft.client.render.entity.state.ProjectileEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Mixin to add magnify scale tracking to ProjectileEntityRenderState.
 */
@Mixin(ProjectileEntityRenderState.class)
public class ProjectileEntityRenderStateMixin implements MagnifyRenderStateAccessor {
    
    @Unique
    private float leo_enchants$magnifyScale = 1.0f;
    
    @Override
    public float leo_enchants$getMagnifyScale() {
        return leo_enchants$magnifyScale;
    }
    
    @Override
    public void leo_enchants$setMagnifyScale(float scale) {
        leo_enchants$magnifyScale = scale;
    }
}

