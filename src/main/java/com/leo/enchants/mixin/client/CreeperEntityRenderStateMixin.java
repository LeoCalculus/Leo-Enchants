package com.leo.enchants.mixin.client;

import com.leo.enchants.accessor.client.MissileCreeperRenderStateAccessor;
import net.minecraft.client.render.entity.state.CreeperEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Mixin to add missile mode fields to the creeper render state.
 */
@Mixin(CreeperEntityRenderState.class)
public class CreeperEntityRenderStateMixin implements MissileCreeperRenderStateAccessor {
    
    @Unique
    private boolean leo_enchants$missileMode = false;
    
    @Unique
    private float leo_enchants$missilePitch = 0.0F;
    
    @Override
    public void leo_enchants$setMissileMode(boolean active) {
        this.leo_enchants$missileMode = active;
    }
    
    @Override
    public boolean leo_enchants$isMissileMode() {
        return this.leo_enchants$missileMode;
    }
    
    @Override
    public void leo_enchants$setMissilePitch(float pitch) {
        this.leo_enchants$missilePitch = pitch;
    }
    
    @Override
    public float leo_enchants$getMissilePitch() {
        return this.leo_enchants$missilePitch;
    }
}
