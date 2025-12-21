package com.leo.enchants.mixin;

import com.leo.enchants.accessor.HeavyArrowAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Mixin to track enhanced skeleton arrows:
 * - 33.3% chance: Heavy arrow (3x damage, considering Power enchantment)
 * - 33.3% chance: Double shot (handled in SkeletonEntityMixin)
 * - 33.3% chance: Tracking arrow (homes in on the target player)
 */
@Mixin(PersistentProjectileEntity.class)
public abstract class HeavyArrowMixin implements HeavyArrowAccessor {
    
    @Unique
    private static final TrackedData<Boolean> HEAVY_ARROW = DataTracker.registerData(
        PersistentProjectileEntity.class, 
        TrackedDataHandlerRegistry.BOOLEAN
    );
    
    @Unique
    private static final TrackedData<Boolean> TRACKING_ARROW = DataTracker.registerData(
        PersistentProjectileEntity.class, 
        TrackedDataHandlerRegistry.BOOLEAN
    );
    
    // Store target UUID as string (empty string = no target)
    @Unique
    private static final TrackedData<String> TRACKING_TARGET = DataTracker.registerData(
        PersistentProjectileEntity.class, 
        TrackedDataHandlerRegistry.STRING
    );
    
    // Tracking arrow configuration
    @Unique
    private static final double TRACKING_STRENGTH = 0.35; // How strongly the arrow homes in (stronger = more aggressive tracking)
    @Unique
    private static final double MAX_TRACKING_DISTANCE = 64.0; // Max distance to track
    
    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void initHeavyArrowData(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(HEAVY_ARROW, false);
        builder.add(TRACKING_ARROW, false);
        builder.add(TRACKING_TARGET, "");
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
    
    @Override
    public void leo_enchants$setTrackingArrow(boolean tracking) {
        PersistentProjectileEntity self = (PersistentProjectileEntity) (Object) this;
        self.getDataTracker().set(TRACKING_ARROW, tracking);
    }
    
    @Override
    public boolean leo_enchants$isTrackingArrow() {
        PersistentProjectileEntity self = (PersistentProjectileEntity) (Object) this;
        return self.getDataTracker().get(TRACKING_ARROW);
    }
    
    @Override
    public void leo_enchants$setTrackingTarget(UUID targetUuid) {
        PersistentProjectileEntity self = (PersistentProjectileEntity) (Object) this;
        self.getDataTracker().set(TRACKING_TARGET, targetUuid != null ? targetUuid.toString() : "");
    }
    
    @Override
    public UUID leo_enchants$getTrackingTarget() {
        PersistentProjectileEntity self = (PersistentProjectileEntity) (Object) this;
        String uuidStr = self.getDataTracker().get(TRACKING_TARGET);
        if (uuidStr == null || uuidStr.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Tick method to update tracking arrow direction
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void tickTrackingArrow(CallbackInfo ci) {
        PersistentProjectileEntity self = (PersistentProjectileEntity) (Object) this;
        
        if (self.getWorld().isClient()) return;
        if (!leo_enchants$isTrackingArrow()) return;
        
        // Don't track when arrow is stuck (velocity near zero)
        double velocityLengthSq = self.getVelocity().lengthSquared();
        if (velocityLengthSq < 0.001) {
            leo_enchants$setTrackingArrow(false);
            return;
        }
        
        UUID targetUuid = leo_enchants$getTrackingTarget();
        if (targetUuid == null) {
            leo_enchants$setTrackingArrow(false);
            return;
        }
        
        ServerWorld serverWorld = (ServerWorld) self.getWorld();
        PlayerEntity target = serverWorld.getPlayerByUuid(targetUuid);
        
        if (target == null || !target.isAlive()) {
            // Target gone, stop tracking
            leo_enchants$setTrackingArrow(false);
            return;
        }
        
        // Check distance
        double distance = self.distanceTo(target);
        if (distance > MAX_TRACKING_DISTANCE) {
            leo_enchants$setTrackingArrow(false);
            return;
        }
        
        // Calculate direction to target (aim for center mass)
        Vec3d arrowPos = self.getPos();
        Vec3d targetPos = target.getPos().add(0, target.getHeight() * 0.5, 0);
        Vec3d directionToTarget = targetPos.subtract(arrowPos).normalize();
        
        // Get current velocity
        Vec3d currentVelocity = self.getVelocity();
        double speed = currentVelocity.length();
        
        // Blend current direction with target direction using stronger tracking
        Vec3d currentDir = currentVelocity.normalize();
        Vec3d newDir = currentDir.multiply(1.0 - TRACKING_STRENGTH).add(directionToTarget.multiply(TRACKING_STRENGTH)).normalize();
        
        // Apply new velocity while maintaining speed (slightly boost speed for tracking arrows)
        Vec3d newVelocity = newDir.multiply(Math.max(speed, 1.5));
        self.setVelocity(newVelocity);
        self.velocityModified = true;
        
        // Spawn tracking particles every few ticks for visual feedback
        if (self.age % 2 == 0) {
            serverWorld.spawnParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                self.getX(), self.getY(), self.getZ(),
                1, 0, 0, 0, 0
            );
        }
    }
    
    /**
     * Apply 3x damage for heavy arrows when hitting an entity (2x bonus damage on top of normal)
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
        
        // Heavy arrow deals 2x extra damage (total 3x)
        // Arrow damage is based on velocity - this includes Power enchantment effects
        // since Power increases velocity/damage through vanilla mechanics
        double arrowDamage = self.getVelocity().length() * 2.0;
        
        if (target instanceof LivingEntity livingTarget) {
            // Apply 2x bonus damage (original + 2x bonus = 3x total)
            float bonusDamage = (float) (arrowDamage * 2.0);
            livingTarget.damage(serverWorld, self.getDamageSources().arrow(self, self.getOwner()), bonusDamage);
        }
    }
}
