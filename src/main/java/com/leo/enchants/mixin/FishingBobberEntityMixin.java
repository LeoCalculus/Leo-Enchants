package com.leo.enchants.mixin;

import com.leo.enchants.logic.GrabHandler;
import com.leo.enchants.logic.HookshotHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingBobberEntity.class)
public abstract class FishingBobberEntityMixin implements HookshotBobberAccessor {

    private static final Identifier GRAB_ID = Identifier.of("leo_enchants", "grab");
    private static final Identifier HOOKSHOT_ID = Identifier.of("leo_enchants", "hookshot");
    
    // Maximum range for hookshot before the hook is cleared
    private static final double HOOKSHOT_MAX_RANGE = 30.0;
    
    // Track if this bobber is a hookshot bobber and attached to a block
    @Unique
    private boolean leo_enchants$isHookshotBobber = false;
    @Unique
    private BlockPos leo_enchants$attachedBlock = null;

    @Shadow
    public abstract PlayerEntity getPlayerOwner();

    /**
     * Mark this bobber as a hookshot bobber.
     */
    @Override
    @Unique
    public void leo_enchants$setHookshotBobber(boolean isHookshot) {
        this.leo_enchants$isHookshotBobber = isHookshot;
    }
    
    /**
     * Check if this is a hookshot bobber.
     */
    @Override
    @Unique
    public boolean leo_enchants$isHookshotBobber() {
        return this.leo_enchants$isHookshotBobber;
    }
    
    /**
     * Set the attached block for hookshot.
     */
    @Override
    @Unique
    public void leo_enchants$setAttachedBlock(BlockPos pos) {
        this.leo_enchants$attachedBlock = pos;
    }
    
    /**
     * Get the attached block.
     */
    @Override
    @Unique
    public BlockPos leo_enchants$getAttachedBlock() {
        return this.leo_enchants$attachedBlock;
    }

    /**
     * Tick handler for hookshot range checking and block attachment.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        FishingBobberEntity bobber = (FishingBobberEntity) (Object) this;
        PlayerEntity player = this.getPlayerOwner();
        
        if (player == null || !leo_enchants$isHookshotBobber) {
            return;
        }
        
        // Check range - if exceeded 30 blocks, discard the bobber
        double distance = bobber.getPos().distanceTo(player.getPos());
        if (distance > HOOKSHOT_MAX_RANGE && leo_enchants$attachedBlock == null) {
            bobber.discard();
            return;
        }
        
        // Check for block collision using raycast (better for fast no-gravity projectiles)
        if (leo_enchants$attachedBlock == null) {
            Vec3d currentPos = bobber.getPos();
            Vec3d velocity = bobber.getVelocity();
            Vec3d nextPos = currentPos.add(velocity);
            
            // Raycast from current position to next position
            BlockHitResult hitResult = bobber.getWorld().raycast(new RaycastContext(
                currentPos,
                nextPos,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                bobber
            ));
            
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                // Hit a block - attach and stop
                leo_enchants$attachedBlock = hitResult.getBlockPos();
                bobber.setPosition(hitResult.getPos());
                bobber.setVelocity(Vec3d.ZERO);
            } else {
                // Also check if bobber is inside a block (collision happened)
                BlockPos bobberBlockPos = bobber.getBlockPos();
                if (!bobber.getWorld().getBlockState(bobberBlockPos).isAir()) {
                    leo_enchants$attachedBlock = bobberBlockPos;
                    bobber.setVelocity(Vec3d.ZERO);
                }
            }
        }
    }

    /**
     * Intercept the use method to implement grab/grappling hook functionality.
     * When a fishing rod with Grab enchantment is retracted, pull the player
     * to the bobber's location instead of fishing.
     */
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onUse(ItemStack usedItem, CallbackInfoReturnable<Integer> cir) {
        FishingBobberEntity bobber = (FishingBobberEntity) (Object) this;
        PlayerEntity player = this.getPlayerOwner();

        if (player == null || usedItem.isEmpty()) {
            return;
        }

        // Check for Hookshot enchantment first
        int hookshotLevel = getHookshotLevel(usedItem);
        if (hookshotLevel > 0) {
            handleHookshotUse(bobber, player, usedItem, cir);
            return;
        }

        // Check if the fishing rod has the Grab enchantment
        int grabLevel = getGrabLevel(usedItem);
        if (grabLevel <= 0) {
            return;
        }

        // Get bobber position
        Vec3d bobberPos = bobber.getPos();
        Vec3d playerPos = player.getPos();

        // Calculate distance
        double distance = playerPos.distanceTo(bobberPos);

        // Only pull if there's meaningful distance
        if (distance > 1.5) {
            // Execute the grab/grapple logic
            GrabHandler.pullPlayerToBobber(player, bobberPos, distance);
            
            // Consume 5 durability on the fishing rod (server-side only)
            if (player instanceof ServerPlayerEntity serverPlayer) {
                // Determine which hand holds the fishing rod
                EquipmentSlot slot = EquipmentSlot.MAINHAND;
                ItemStack mainHand = player.getStackInHand(Hand.MAIN_HAND);
                if (getGrabLevel(mainHand) <= 0) {
                    slot = EquipmentSlot.OFFHAND;
                }
                usedItem.damage(5, serverPlayer, slot);
            }
        }

        // Remove the bobber
        bobber.discard();

        // Return 0 to indicate no fishing happened, and cancel normal behavior
        cir.setReturnValue(0);
    }
    
    /**
     * Handle hookshot enchantment use - pull player to attached block.
     */
    @Unique
    private void handleHookshotUse(FishingBobberEntity bobber, PlayerEntity player, ItemStack usedItem, CallbackInfoReturnable<Integer> cir) {
        Vec3d bobberPos = bobber.getPos();
        Vec3d playerPos = player.getPos();
        double distance = playerPos.distanceTo(bobberPos);
        
        // Only pull if attached to a block and there's meaningful distance
        if (leo_enchants$attachedBlock != null && distance > 1.5) {
            // Execute hookshot pull logic
            Vec3d targetPos = Vec3d.ofCenter(leo_enchants$attachedBlock);
            HookshotHandler.pullPlayerToBlock(player, targetPos, leo_enchants$attachedBlock);
            
            // Consume 2 durability on the fishing rod (server-side only)
            if (player instanceof ServerPlayerEntity serverPlayer) {
                EquipmentSlot slot = EquipmentSlot.MAINHAND;
                ItemStack mainHand = player.getStackInHand(Hand.MAIN_HAND);
                if (getHookshotLevel(mainHand) <= 0) {
                    slot = EquipmentSlot.OFFHAND;
                }
                usedItem.damage(2, serverPlayer, slot);
            }
        }
        
        // Remove the bobber
        bobber.discard();
        
        // Return 0 to indicate no fishing happened, and cancel normal behavior
        cir.setReturnValue(0);
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

