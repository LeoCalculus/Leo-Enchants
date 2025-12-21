package com.leo.enchants.mixin;

import com.leo.enchants.accessor.MagnifyArrowAccessor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to apply bonus damage for Magnify enchantment on arrow hit.
 * Damage multipliers (applied on top of all existing damage including Power):
 * - Level 1: +100% (2x total)
 * - Level 2: +150% (2.5x total)
 * - Level 3: +200% (3x total)
 */
@Mixin(PersistentProjectileEntity.class)
public abstract class ArrowEntityMixin {
    
    @Unique
    private static final Identifier MAGNIFY_ID = Identifier.of("leo_enchants", "magnify");
    
    /**
     * Apply bonus damage when the arrow hits an entity.
     * We apply additional damage equal to a percentage of what would have been dealt.
     */
    @Inject(method = "onEntityHit", at = @At("HEAD"))
    private void applyMagnifyDamage(EntityHitResult entityHitResult, CallbackInfo ci) {
        PersistentProjectileEntity self = (PersistentProjectileEntity) (Object) this;
        
        // Get the magnify level from the arrow
        int level = 0;
        if (self instanceof MagnifyArrowAccessor accessor) {
            level = accessor.leo_enchants$getMagnifyLevel();
        }
        
        // Fallback: check the owner's bow if the arrow wasn't tagged
        if (level == 0) {
            Entity owner = self.getOwner();
            if (owner instanceof LivingEntity livingOwner) {
                level = getMagnifyLevelFromBow(livingOwner);
            }
        }
        
        if (level <= 0) {
            return;
        }
        
        Entity target = entityHitResult.getEntity();
        if (target == null || !(target.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        
        // Calculate bonus damage multiplier:
        // Level 1: +100% (multiply by 1.0)
        // Level 2: +150% (multiply by 1.5)
        // Level 3: +200% (multiply by 2.0)
        float bonusMultiplier = 0.5f + (level * 0.5f); // 1.0, 1.5, 2.0
        
        // Get the arrow's current damage (includes Power enchantment effects)
        // Arrow damage is typically 2.0 base, modified by velocity and Power
        // We'll estimate the damage and apply bonus
        double arrowDamage = self.getVelocity().length() * 2.0; // Approximate arrow damage formula
        
        // Apply the bonus damage
        float bonusDamage = (float) (arrowDamage * bonusMultiplier);
        
        if (target instanceof LivingEntity livingTarget && bonusDamage > 0) {
            livingTarget.damage(serverWorld, self.getDamageSources().arrow(self, self.getOwner()), bonusDamage);
        }
    }
    
    @Unique
    private int getMagnifyLevelFromBow(LivingEntity entity) {
        // Check main hand
        ItemStack mainHand = entity.getStackInHand(Hand.MAIN_HAND);
        int level = getMagnifyLevel(mainHand);
        if (level > 0) {
            return level;
        }
        
        // Check off hand
        ItemStack offHand = entity.getStackInHand(Hand.OFF_HAND);
        return getMagnifyLevel(offHand);
    }
    
    @Unique
    private int getMagnifyLevel(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        
        ItemEnchantmentsComponent enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null) {
            return 0;
        }

        for (var entry : enchantments.getEnchantmentEntries()) {
            if (entry.getKey().matchesId(MAGNIFY_ID)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }
}
