package com.leo.enchants.mixin;

import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.CreeperEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to add missile mode data tracking to creepers for client-side rendering.
 */
@Mixin(CreeperEntity.class)
public abstract class CreeperMissileDataMixin implements MissileCreeperAccessor {
    
    @Unique
    private static final TrackedData<Boolean> MISSILE_MODE = DataTracker.registerData(
        CreeperEntity.class, 
        TrackedDataHandlerRegistry.BOOLEAN
    );
    
    @Unique
    private static final TrackedData<Float> MISSILE_PITCH = DataTracker.registerData(
        CreeperEntity.class, 
        TrackedDataHandlerRegistry.FLOAT
    );
    
    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void initMissileData(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(MISSILE_MODE, false);
        builder.add(MISSILE_PITCH, 0.0F);
    }
    
    @Override
    public void leo_enchants$setMissileMode(boolean active) {
        CreeperEntity self = (CreeperEntity) (Object) this;
        self.getDataTracker().set(MISSILE_MODE, active);
    }
    
    @Override
    public boolean leo_enchants$isMissileMode() {
        CreeperEntity self = (CreeperEntity) (Object) this;
        return self.getDataTracker().get(MISSILE_MODE);
    }
    
    @Override
    public void leo_enchants$setMissilePitch(float pitch) {
        CreeperEntity self = (CreeperEntity) (Object) this;
        self.getDataTracker().set(MISSILE_PITCH, pitch);
    }
    
    @Override
    public float leo_enchants$getMissilePitch() {
        CreeperEntity self = (CreeperEntity) (Object) this;
        return self.getDataTracker().get(MISSILE_PITCH);
    }
}
