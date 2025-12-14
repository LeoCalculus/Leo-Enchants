package com.leo.enchants.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingRodItem.class)
public class FishingRodItemMixin {

    private static final Identifier GRAB_ID = Identifier.of("leo_enchants", "grab");
    private static final Identifier HOOKSHOT_ID = Identifier.of("leo_enchants", "hookshot");
    
    // Speed multiplier for Grab-enchanted fishing rods
    private static final double GRAB_SPEED_MULTIPLIER = 2.0;
    
    // Speed multiplier for Hookshot-enchanted fishing rods (very fast, like a line)
    private static final double HOOKSHOT_SPEED_MULTIPLIER = 8.0;

    /**
     * After the fishing bobber is spawned, boost its velocity based on enchantment.
     * This runs on BOTH client and server to ensure proper rendering sync.
     */
    @Inject(method = "use", at = @At("RETURN"))
    private void boostBobberVelocity(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<?> cir) {
        ItemStack stack = user.getStackInHand(hand);
        if (stack.isEmpty()) {
            return;
        }
        
        // Find the player's fishing bobber
        FishingBobberEntity bobber = user.fishHook;
        if (bobber == null) {
            return;
        }
        
        // Check for Hookshot enchantment first (higher priority)
        int hookshotLevel = getHookshotLevel(stack);
        if (hookshotLevel > 0) {
            // Mark the bobber as a hookshot bobber (on both client and server)
            if (bobber instanceof HookshotBobberAccessor accessor) {
                accessor.leo_enchants$setHookshotBobber(true);
            }
            
            // Calculate straight line velocity from player's look direction
            // This replaces the random spread that fishing bobbers normally have
            Vec3d lookDirection = user.getRotationVec(1.0F);
            double speed = 10.0; // Super fast, instant line speed
            
            // Set velocity directly to look direction (no random spread)
            // Must be set on both client and server for proper rendering
            bobber.setVelocity(lookDirection.multiply(speed));
            
            // Disable gravity for straight line travel
            bobber.setNoGravity(true);
            
            // Mark velocity as modified to ensure sync
            bobber.velocityModified = true;
            return;
        }
        
        // Check for Grab enchantment (server only for durability handling)
        if (world.isClient()) {
            return;
        }
        
        int grabLevel = getGrabLevel(stack);
        if (grabLevel > 0) {
            Vec3d velocity = bobber.getVelocity();
            bobber.setVelocity(velocity.multiply(GRAB_SPEED_MULTIPLIER));
        }
    }
    
    /**
     * Get the level of Grab enchantment on the item
     */
    @Unique
    private int getGrabLevel(ItemStack stack) {
        ItemEnchantmentsComponent enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null) {
            return 0;
        }

        for (var entry : enchantments.getEnchantmentEntries()) {
            if (entry.getKey().matchesId(GRAB_ID)) {
                return entry.getIntValue();
            }
        }

        return 0;
    }
    
    /**
     * Get the level of Hookshot enchantment on the item
     */
    @Unique
    private int getHookshotLevel(ItemStack stack) {
        ItemEnchantmentsComponent enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null) {
            return 0;
        }

        for (var entry : enchantments.getEnchantmentEntries()) {
            if (entry.getKey().matchesId(HOOKSHOT_ID)) {
                return entry.getIntValue();
            }
        }

        return 0;
    }
}


