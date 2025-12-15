package com.leo.enchants.logic;

import com.leo.enchants.entity.DigitDisintegrationEntity;
import com.leo.enchants.entity.ModEntities;
import com.leo.enchants.item.ModItems;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the de-enchant item's attack events.
 * When a player attacks an entity or block with the de-enchant item,
 * it creates a digital disintegration effect and removes the target.
 */
public class DeEnchantHandler {
    
    // Cooldown tracking: player UUID -> last use time (world ticks)
    private static final Map<UUID, Long> lastUseTime = new HashMap<>();
    
    // Cooldown in ticks (10 ticks = 0.5 seconds)
    private static final long COOLDOWN_TICKS = 10;
    
    /**
     * Checks if a player can use the de-enchant item (cooldown check).
     */
    private static boolean canUse(PlayerEntity player, World world) {
        UUID playerId = player.getUuid();
        long currentTime = world.getTime();
        
        Long lastTime = lastUseTime.get(playerId);
        if (lastTime == null || currentTime - lastTime >= COOLDOWN_TICKS) {
            lastUseTime.put(playerId, currentTime);
            return true;
        }
        return false;
    }
    
    /**
     * Registers the event handlers for the de-enchant item.
     */
    public static void register() {
        // Handle entity attacks
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            ItemStack stack = player.getStackInHand(hand);
            
            if (stack.isOf(ModItems.DE_ENCHANT)) {
                // Don't affect other players
                if (entity instanceof PlayerEntity) {
                    return ActionResult.PASS;
                }
                
                if (!world.isClient && world instanceof ServerWorld serverWorld) {
                    // Check cooldown
                    if (!canUse(player, world)) {
                        return ActionResult.FAIL;
                    }
                    
                    // Spawn the digital disintegration effect
                    spawnDisintegrationEffect(serverWorld, entity.getX(), entity.getY(), entity.getZ(),
                            entity.getWidth(), entity.getHeight());
                    
                    // Play glitchy sound effect
                    serverWorld.playSound(null, entity.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                            SoundCategory.PLAYERS, 1.0f, 2.0f);
                    serverWorld.playSound(null, entity.getBlockPos(), SoundEvents.BLOCK_BEACON_DEACTIVATE,
                            SoundCategory.PLAYERS, 0.5f, 1.5f);
                    
                    // Remove the entity
                    entity.discard();
                    
                    // Consume the item (reduce count by 1)
                    if (!player.isCreative()) {
                        stack.decrement(1);
                    }
                }
                
                return ActionResult.SUCCESS;
            }
            
            return ActionResult.PASS;
        });
        
        // Handle block attacks (left-click on blocks)
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            ItemStack stack = player.getStackInHand(hand);
            
            if (stack.isOf(ModItems.DE_ENCHANT)) {
                if (!world.isClient && world instanceof ServerWorld serverWorld) {
                    // Check cooldown
                    if (!canUse(player, world)) {
                        return ActionResult.FAIL;
                    }
                    
                    // Only destroy if block is not air
                    if (!world.getBlockState(pos).isAir()) {
                        // Spawn the digital disintegration effect at block position
                        spawnDisintegrationEffect(serverWorld, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                1.0f, 1.0f);
                        
                        // Play glitchy sound effect
                        serverWorld.playSound(null, pos, SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                                SoundCategory.PLAYERS, 1.0f, 2.0f);
                        serverWorld.playSound(null, pos, SoundEvents.BLOCK_BEACON_DEACTIVATE,
                                SoundCategory.PLAYERS, 0.5f, 1.5f);
                        
                        // Remove the block without dropping items
                        serverWorld.removeBlock(pos, false);
                        
                        // Consume the item (reduce count by 1)
                        if (!player.isCreative()) {
                            stack.decrement(1);
                        }
                    }
                }
                
                return ActionResult.SUCCESS;
            }
            
            return ActionResult.PASS;
        });
    }
    
    /**
     * Checks if a non-player entity is holding the de-enchant item and drops it.
     * Should be called from server tick events.
     */
    public static void checkNonPlayerHolding(LivingEntity entity, ServerWorld world) {
        if (entity instanceof PlayerEntity) {
            return; // Players can hold the item
        }
        
        // Check main hand
        ItemStack mainHand = entity.getMainHandStack();
        if (mainHand.isOf(ModItems.DE_ENCHANT)) {
            entity.dropStack(world, mainHand.copy());
            mainHand.setCount(0);
        }
        
        // Check off hand
        ItemStack offHand = entity.getOffHandStack();
        if (offHand.isOf(ModItems.DE_ENCHANT)) {
            entity.dropStack(world, offHand.copy());
            offHand.setCount(0);
        }
    }
    
    /**
     * Spawns multiple digit disintegration entities to create the visual effect.
     */
    private static void spawnDisintegrationEffect(ServerWorld world, double x, double y, double z,
                                                   float width, float height) {
        int numDigits = (int) (15 + (width * height * 10)); // Scale with entity size
        numDigits = Math.min(numDigits, 50); // Cap at 50 for performance
        
        for (int i = 0; i < numDigits; i++) {
            DigitDisintegrationEntity digit = new DigitDisintegrationEntity(ModEntities.DIGIT_DISINTEGRATION, world);
            
            // Random position within the target's bounding box
            double offsetX = (world.random.nextDouble() - 0.5) * width;
            double offsetY = world.random.nextDouble() * height;
            double offsetZ = (world.random.nextDouble() - 0.5) * width;
            
            digit.setPosition(x + offsetX, y + offsetY, z + offsetZ);
            digit.setRandomDigit(world.random.nextBoolean()); // true = 1, false = 0
            digit.setRandomVelocity(world.random);
            
            world.spawnEntity(digit);
        }
    }
}
