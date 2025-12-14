package com.leo.enchants.logic;

import com.leo.enchants.LeoEnchantsMod;
import com.leo.enchants.entity.GiantSwordEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.registry.Registries;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class GiantSwordLogic {
    
    // Durability costs per level
    private static final int DURABILITY_COST_LEVEL_1 = 20;
    private static final int DURABILITY_COST_LEVEL_2 = 50;
    private static final int DURABILITY_COST_LEVEL_3 = 500;
    
    // Health costs per level (in half-hearts, so 2 = 1 heart)
    private static final float HEALTH_COST_LEVEL_1 = 0.0f;
    private static final float HEALTH_COST_LEVEL_2 = 2.0f;  // 1 heart
    private static final float HEALTH_COST_LEVEL_3 = 12.0f; // 6 hearts
    
    // Sword spawn height offset
    private static final int SPAWN_HEIGHT_OFFSET = 100; // Spawn 100 blocks above target
    
    /**
     * Activates the Giant enchantment
     * 
     * @param player The player activating the enchantment
     * @param world The world
     * @param level The enchantment level (1-3)
     * @return true if activated successfully
     */
    public static boolean activate(PlayerEntity player, World world, int level) {
        if (world.isClient) return false;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return false;
        if (!(world instanceof ServerWorld serverWorld)) return false;
        
        ItemStack heldItem = player.getStackInHand(Hand.MAIN_HAND);
        
        // Check if the sword has enough durability
        int durabilityCost = getDurabilityCostForLevel(level);
        int currentDurability = heldItem.getMaxDamage() - heldItem.getDamage();
        
        // For level 3, if durability is less than 500, still allow one use
        if (level < 3 && currentDurability < durabilityCost) {
            // Not enough durability for level 1-2
            return false;
        }
        
        // Get the target position
        Vec3d targetPos = getTargetPosition(player, world);
        if (targetPos == null) {
            return false;
        }
        
        // Apply health cost first (except for level 1)
        float healthCost = getHealthCostForLevel(level);
        if (healthCost > 0) {
            if (player.getHealth() <= healthCost) {
                // Can't use if it would kill the player
                return false;
            }
            player.setHealth(player.getHealth() - healthCost);
        }
        
        // Apply durability cost
        applyDurabilityCost(serverPlayer, heldItem, level);
        
        // Get the sword type from the held item
        String swordType = Registries.ITEM.getId(heldItem.getItem()).toString();
        
        // Spawn the giant sword entity
        spawnGiantSword(serverWorld, targetPos, level, player, swordType);
        
        // Play activation sound
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 1.0f, 1.5f);
        
        // Add cooldown (prevent spam)
        player.getItemCooldownManager().set(heldItem, 100); // 5 second cooldown
        
        LeoEnchantsMod.LOGGER.info("Player {} activated Giant enchantment level {}", 
            player.getName().getString(), level);
        
        return true;
    }
    
    private static Vec3d getTargetPosition(PlayerEntity player, World world) {
        Vec3d eyePos = player.getCameraPosVec(1.0f);
        Vec3d lookVec = player.getRotationVector();
        double maxDistance = 100.0; // Max targeting distance
        
        Vec3d endPos = eyePos.add(lookVec.multiply(maxDistance));
        
        // Raycast to find target block
        BlockHitResult hitResult = world.raycast(new RaycastContext(
            eyePos,
            endPos,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            player
        ));
        
        if (hitResult.getType() == HitResult.Type.MISS) {
            // If no block hit, target the max distance position
            return endPos;
        }
        
        // Return the hit position
        return Vec3d.ofCenter(hitResult.getBlockPos());
    }
    
    private static void spawnGiantSword(ServerWorld world, Vec3d targetPos, int level, PlayerEntity owner, String swordType) {
        // Calculate spawn position (high in the sky)
        double spawnY = Math.min(targetPos.y + SPAWN_HEIGHT_OFFSET, world.getTopYInclusive() - 10);
        
        GiantSwordEntity sword = new GiantSwordEntity(
            world,
            targetPos.x,
            spawnY,
            targetPos.z,
            level,
            owner.getUuid(),
            swordType
        );
        
        world.spawnEntity(sword);
        
        LeoEnchantsMod.LOGGER.info("Spawned Giant {} at ({}, {}, {}) targeting ({}, {}, {})",
            swordType, targetPos.x, spawnY, targetPos.z, targetPos.x, targetPos.y, targetPos.z);
    }
    
    private static int getDurabilityCostForLevel(int level) {
        return switch (level) {
            case 1 -> DURABILITY_COST_LEVEL_1;
            case 2 -> DURABILITY_COST_LEVEL_2;
            case 3 -> DURABILITY_COST_LEVEL_3;
            default -> DURABILITY_COST_LEVEL_1;
        };
    }
    
    private static float getHealthCostForLevel(int level) {
        return switch (level) {
            case 1 -> HEALTH_COST_LEVEL_1;
            case 2 -> HEALTH_COST_LEVEL_2;
            case 3 -> HEALTH_COST_LEVEL_3;
            default -> HEALTH_COST_LEVEL_1;
        };
    }
    
    private static void applyDurabilityCost(ServerPlayerEntity player, ItemStack stack, int level) {
        if (stack.isEmpty() || !stack.isDamageable()) {
            return;
        }
        
        int cost = getDurabilityCostForLevel(level);
        int currentDurability = stack.getMaxDamage() - stack.getDamage();
        
        // For level 3, if durability < 500, use all remaining durability
        if (level >= 3 && currentDurability < cost) {
            cost = currentDurability;
        }
        
        // Apply the damage
        stack.damage(cost, player, player.getPreferredEquipmentSlot(stack));
    }
}
