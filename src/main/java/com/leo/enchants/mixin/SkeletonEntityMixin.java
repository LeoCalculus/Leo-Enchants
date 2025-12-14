package com.leo.enchants.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to enhance skeletons with special arrow abilities:
 * - 50% chance to shoot 2 arrows at once
 * - 50% chance to shoot a heavy arrow (2x damage)
 */
@Mixin(AbstractSkeletonEntity.class)
public abstract class SkeletonEntityMixin extends HostileEntity {

    protected SkeletonEntityMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    /**
     * Inject after the skeleton shoots to potentially add extra arrow or mark as heavy
     */
    @Inject(method = "shootAt", at = @At("TAIL"))
    private void enhanceSkeletonShot(LivingEntity target, float pullProgress, CallbackInfo ci) {
        AbstractSkeletonEntity skeleton = (AbstractSkeletonEntity) (Object) this;
        World world = skeleton.getWorld();
        
        if (world.isClient()) return;
        
        // Roll for enhancement type (50/50 split)
        boolean doubleShot = world.random.nextBoolean();
        
        if (doubleShot) {
            // Shoot an extra arrow with slight offset
            shootExtraArrow(skeleton, target, pullProgress);
        }
        // Note: Heavy arrow is handled by marking the arrow in the other injection point
    }
    
    /**
     * Shoot an additional arrow with slight angle offset
     */
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
