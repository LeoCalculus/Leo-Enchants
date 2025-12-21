package com.leo.enchants.mixin;

import com.leo.enchants.accessor.MagnifyArrowAccessor;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to add magnify level tracking to projectile entities with proper client sync.
 */
@Mixin(PersistentProjectileEntity.class)
public abstract class PersistentProjectileEntityMixin implements MagnifyArrowAccessor {
    
    @Unique
    private static final TrackedData<Integer> MAGNIFY_LEVEL = DataTracker.registerData(
        PersistentProjectileEntity.class, 
        TrackedDataHandlerRegistry.INTEGER
    );
    
    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void initMagnifyData(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(MAGNIFY_LEVEL, 0);
    }
    
    @Override
    public void leo_enchants$setMagnifyLevel(int level) {
        PersistentProjectileEntity self = (PersistentProjectileEntity) (Object) this;
        self.getDataTracker().set(MAGNIFY_LEVEL, level);
        
        // Update the entity's dimensions based on scale
        self.calculateDimensions();
    }
    
    @Override
    public int leo_enchants$getMagnifyLevel() {
        PersistentProjectileEntity self = (PersistentProjectileEntity) (Object) this;
        return self.getDataTracker().get(MAGNIFY_LEVEL);
    }
    
    @Override
    public float leo_enchants$getMagnifyScale() {
        int level = leo_enchants$getMagnifyLevel();
        if (level <= 0) {
            return 1.0f;
        }
        // 3x, 5x, 7x for levels 1, 2, 3
        return 1 + (level * 2);
    }
}
