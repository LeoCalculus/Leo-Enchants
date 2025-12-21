package com.leo.enchants.mixin;

import com.leo.enchants.accessor.HeavyArrowAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to track heavy arrows and apply 2x damage.
 * Heavy arrows are shot by enhanced skeletons with 50% chance.
 */
@Mixin(PersistentProjectileEntity.class)
public abstract class HeavyArrowMixin implements HeavyArrowAccessor {
    
    @Unique
    private static final TrackedData<Boolean> HEAVY_ARROW = DataTracker.registerData(
        PersistentProjectileEntity.class, 
        TrackedDataHandlerRegistry.BOOLEAN
    );
    
    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void initHeavyArrowData(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(HEAVY_ARROW, false);
    }
    
    @Override
    public void leo_enchants$setHeavyArrow(boolean heavy) {
        PersistentProjectileEntity self = (PersistentProjectileEntity) (Object) this;
        self.getDataTracker().set(HEAVY_ARROW, heavy);
    }
    
    @Override
    public boolean leo_enchants$isHeavyArrow() {
        PersistentProjectileEntity self = (PersistentProjectileEntity) (Object) this;
        return self.getDataTracker().get(HEAVY_ARROW);
    }
    
    /**
     * When arrow is first set in motion by a skeleton, 50% chance to become heavy
     */
    @Inject(method = "setVelocity(DDDFF)V", at = @At("HEAD"))
    private void checkForHeavyArrow(double x, double y, double z, float speed, float divergence, CallbackInfo ci) {
        PersistentProjectileEntity self = (PersistentProjectileEntity) (Object) this;
        Entity owner = self.getOwner();
        
        // Only skeleton arrows can be heavy
        if (owner instanceof AbstractSkeletonEntity skeleton) {
            // 50% chance to be heavy (independent of double shot)
            if (skeleton.getWorld().random.nextBoolean()) {
                leo_enchants$setHeavyArrow(true);
            }
        }
    }
    
    /**
     * Apply 2x damage for heavy arrows when hitting an entity
     */
    @Inject(method = "onEntityHit", at = @At("HEAD"))
    private void applyHeavyArrowDamage(EntityHitResult entityHitResult, CallbackInfo ci) {
        PersistentProjectileEntity self = (PersistentProjectileEntity) (Object) this;
        
        if (!leo_enchants$isHeavyArrow()) {
            return;
        }
        
        Entity target = entityHitResult.getEntity();
        if (target == null || !(target.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        
        // Heavy arrow deals 1x extra damage (total 2x)
        // Estimate arrow damage based on velocity (similar to vanilla calculation)
        double arrowDamage = self.getVelocity().length() * 2.0;
        
        if (target instanceof LivingEntity livingTarget) {
            // Apply the bonus damage (equal to original = 2x total)
            float bonusDamage = (float) arrowDamage;
            livingTarget.damage(serverWorld, self.getDamageSources().arrow(self, self.getOwner()), bonusDamage);
        }
    }
}
