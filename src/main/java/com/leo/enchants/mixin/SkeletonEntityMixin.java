package com.leo.enchants.mixin;

import com.leo.enchants.accessor.HeavyArrowAccessor;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Mixin to enhance skeletons with special arrow abilities:
 * - 33.3% chance to shoot a heavy arrow (3x damage, considers Power enchantment)
 * - 33.3% chance to shoot 2 arrows at once (double shot)
 * - 33.3% chance to shoot a tracking arrow (homes in on the player)
 */
@Mixin(AbstractSkeletonEntity.class)
public abstract class SkeletonEntityMixin extends HostileEntity {

    // Enhancement type constants
    @Unique
    private static final int ENHANCEMENT_HEAVY = 0;
    @Unique
    private static final int ENHANCEMENT_DOUBLE = 1;
    @Unique
    private static final int ENHANCEMENT_TRACKING = 2;

    protected SkeletonEntityMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    /**
     * Inject after the skeleton shoots to apply enhancements
     */
    @Inject(method = "shootAt", at = @At("TAIL"))
    private void enhanceSkeletonShot(LivingEntity target, float pullProgress, CallbackInfo ci) {
        AbstractSkeletonEntity skeleton = (AbstractSkeletonEntity) (Object) this;
        World world = skeleton.getWorld();
        
        if (world.isClient()) return;
        
        // Roll for enhancement type (33.3% each)
        int enhancement = world.random.nextInt(3);
        
        // Find recently spawned arrows from this skeleton (age <= 5 to ensure we catch it)
        List<PersistentProjectileEntity> nearbyArrows = world.getEntitiesByClass(
            PersistentProjectileEntity.class,
            skeleton.getBoundingBox().expand(8.0),
            arrow -> arrow.getOwner() == skeleton && arrow.age <= 5
        );
        
        // Apply enhancement to the arrow(s)
        for (PersistentProjectileEntity arrow : nearbyArrows) {
            if (arrow instanceof HeavyArrowAccessor accessor) {
                // Only apply if not already enhanced
                if (!accessor.leo_enchants$isHeavyArrow() && !accessor.leo_enchants$isTrackingArrow()) {
                    switch (enhancement) {
                        case ENHANCEMENT_HEAVY:
                            accessor.leo_enchants$setHeavyArrow(true);
                            break;
                        case ENHANCEMENT_TRACKING:
                            // Only enable tracking if target is a player (we can track by UUID)
                            if (target instanceof PlayerEntity player) {
                                accessor.leo_enchants$setTrackingArrow(true);
                                accessor.leo_enchants$setTrackingTarget(player.getUuid());
                            } else {
                                // If not a player, fall back to heavy arrow instead
                                accessor.leo_enchants$setHeavyArrow(true);
                            }
                            break;
                        case ENHANCEMENT_DOUBLE:
                            // Double shot - shoot an extra arrow
                            shootExtraArrow(skeleton, target, pullProgress);
                            break;
                    }
                }
            }
        }
    }
    
    /**
     * Shoot an additional arrow with slight angle offset (for double shot)
     */
    @Unique
    private void shootExtraArrow(AbstractSkeletonEntity skeleton, LivingEntity target, float pullProgress) {
        World world = skeleton.getWorld();
        
        ItemStack bow = skeleton.getMainHandStack();
        if (!bow.isOf(Items.BOW)) {
            bow = new ItemStack(Items.BOW);
        }
        
        // Create extra arrow
        ItemStack arrowStack = new ItemStack(Items.ARROW);
        PersistentProjectileEntity arrow = ProjectileUtil.createArrowProjectile(skeleton, arrowStack, pullProgress, bow);
        
        // Calculate direction to target with offset
        double dx = target.getX() - skeleton.getX();
        double dy = target.getBodyY(0.3333333333333333D) - arrow.getY();
        double dz = target.getZ() - skeleton.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        
        // Add slight random offset for spread effect
        double offsetX = (world.random.nextDouble() - 0.5) * 0.5;
        double offsetZ = (world.random.nextDouble() - 0.5) * 0.5;
        
        arrow.setVelocity(
            dx + offsetX, 
            dy + horizontalDist * 0.2, 
            dz + offsetZ, 
            1.6F, 
            (float)(14 - world.getDifficulty().getId() * 4)
        );
        
        // Play sound
        skeleton.playSound(SoundEvents.ENTITY_SKELETON_SHOOT, 1.0F, 1.0F / (world.random.nextFloat() * 0.4F + 0.8F));
        
        // Spawn the extra arrow
        world.spawnEntity(arrow);
    }
}
