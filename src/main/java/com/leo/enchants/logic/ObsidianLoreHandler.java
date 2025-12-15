package com.leo.enchants.logic;

import com.leo.enchants.LeoEnchantsMod;
import com.leo.enchants.entity.ModEntities;
import com.leo.enchants.entity.ObsidianBridgeEntity;
import com.leo.enchants.entity.ObsidianStrikeEntity;
import com.leo.enchants.item.ModItems;
import com.leo.enchants.item.ObsidianLoreItem;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/**
 * Handler for Obsidian Lore item functionality.
 * 
 * Build Mode:
 * - First right-click: Store first position (player's aimed block)
 * - Second right-click: Build obsidian bridge to second position
 * - Max distance: 50 blocks
 * - Bridge disappears after 20 seconds
 * 
 * Attack Mode:
 * - Right-click: Launch obsidian strike toward aimed entity/block
 * - Damage scales with distance
 * - Strike disappears after 10 seconds
 * 
 * Shift + Right-click: Toggle between modes
 */
public class ObsidianLoreHandler {
    
    private static final double MAX_BRIDGE_DISTANCE = 50.0;
    private static final double MAX_RAYCAST_DISTANCE = 100.0;
    
    /**
     * Register event handlers for the Obsidian Lore item
     */
    public static void register() {
        // Handle right-click on blocks
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            ItemStack stack = player.getStackInHand(hand);
            
            if (stack.isOf(ModItems.OBSIDIAN_LORE)) {
                return handleUse(player, world, hand, stack, hitResult);
            }
            
            return ActionResult.PASS;
        });
        
        // Handle right-click in air (for raycast-based targeting)
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            
            if (stack.isOf(ModItems.OBSIDIAN_LORE)) {
                // Perform raycast to find target
                HitResult hitResult = raycast(player, MAX_RAYCAST_DISTANCE);
                
                if (hitResult.getType() != HitResult.Type.MISS) {
                    return handleUse(player, world, hand, stack, hitResult);
                } else {
                    // No target found
                    player.sendMessage(Text.literal("§5[Obsidian Mastery] §7No target in range"), true);
                    return ActionResult.FAIL;
                }
            }
            
            return ActionResult.PASS;
        });
        
        LeoEnchantsMod.LOGGER.info("Registered Obsidian Lore event handlers");
    }
    
    /**
     * Handle the use action for the Obsidian Lore item
     */
    private static ActionResult handleUse(PlayerEntity player, World world, Hand hand, 
                                          ItemStack stack, HitResult hitResult) {
        
        // Shift + Right-click: Toggle mode
        if (player.isSneaking()) {
            ObsidianLoreItem.toggleMode(stack, player);
            return ActionResult.SUCCESS;
        }
        
        // Get current mode
        int mode = ObsidianLoreItem.getMode(stack);
        
        if (mode == ObsidianLoreItem.BUILD_MODE) {
            return handleBuildMode(player, world, stack, hitResult);
        } else {
            return handleAttackMode(player, world, stack, hitResult);
        }
    }
    
    /**
     * Handle build mode - create obsidian bridges
     */
    private static ActionResult handleBuildMode(PlayerEntity player, World world, 
                                                ItemStack stack, HitResult hitResult) {
        Vec3d targetPos = getTargetPosition(hitResult);
        if (targetPos == null) {
            player.sendMessage(Text.literal("§5[Obsidian Mastery] §7Invalid target position"), true);
            return ActionResult.FAIL;
        }
        
        // Check if we have a first position stored
        if (!ObsidianLoreItem.hasFirstPosition(stack)) {
            // Store first position
            ObsidianLoreItem.setFirstPosition(stack, targetPos.x, targetPos.y, targetPos.z);
            player.sendMessage(Text.literal("§5[Obsidian Mastery] §aFirst point set! §7Right-click again to build bridge"), true);
            return ActionResult.SUCCESS;
        } else {
            // We have a first position - build the bridge!
            double[] firstPos = ObsidianLoreItem.getFirstPosition(stack);
            Vec3d startPos = new Vec3d(firstPos[0], firstPos[1], firstPos[2]);
            Vec3d endPos = targetPos;
            
            // Check distance
            double distance = startPos.distanceTo(endPos);
            if (distance > MAX_BRIDGE_DISTANCE) {
                player.sendMessage(Text.literal("§5[Obsidian Mastery] §cToo far! §7Max distance: " + (int) MAX_BRIDGE_DISTANCE + " blocks"), true);
                return ActionResult.FAIL;
            }
            
            if (distance < 2) {
                player.sendMessage(Text.literal("§5[Obsidian Mastery] §cPoints are too close!"), true);
                return ActionResult.FAIL;
            }
            
            // Create bridge entity on server
            if (!world.isClient && world instanceof ServerWorld serverWorld) {
                ObsidianBridgeEntity bridge = new ObsidianBridgeEntity(
                    world, startPos, endPos, player.getUuid()
                );
                serverWorld.spawnEntity(bridge);
                
                player.sendMessage(Text.literal("§5[Obsidian Mastery] §dObsidian bridge forming! §7(" + (int) distance + " blocks)"), true);
                LeoEnchantsMod.LOGGER.info("Player {} created obsidian bridge from {} to {} ({} blocks)", 
                    player.getName().getString(), startPos, endPos, (int) distance);
            }
            
            // Clear the first position
            ObsidianLoreItem.clearFirstPosition(stack);
            
            return ActionResult.SUCCESS;
        }
    }
    
    /**
     * Handle attack mode - launch obsidian strikes
     */
    private static ActionResult handleAttackMode(PlayerEntity player, World world, 
                                                 ItemStack stack, HitResult hitResult) {
        Vec3d targetPos = getTargetPosition(hitResult);
        if (targetPos == null) {
            player.sendMessage(Text.literal("§5[Obsidian Mastery] §7No target found!"), true);
            return ActionResult.FAIL;
        }
        
        // Start position is just in front of the player
        Vec3d startPos = player.getEyePos().add(player.getRotationVector().multiply(1.5));
        startPos = new Vec3d(startPos.x, startPos.y - 0.5, startPos.z); // Slightly lower
        
        double distance = startPos.distanceTo(targetPos);
        
        // Get target entity if any
        Entity targetEntity = null;
        if (hitResult instanceof EntityHitResult entityHit) {
            targetEntity = entityHit.getEntity();
        }
        
        // Create strike entity on server
        if (!world.isClient && world instanceof ServerWorld serverWorld) {
            ObsidianStrikeEntity strike = new ObsidianStrikeEntity(
                world, startPos, targetPos, player.getUuid(), targetEntity
            );
            serverWorld.spawnEntity(strike);
            
            float damage = 5.0f + (float) distance * 0.5f;
            damage = Math.min(damage, 50.0f);
            
            player.sendMessage(Text.literal("§5[Obsidian Mastery] §4Strike launched! §7(" + (int) distance + " blocks, ~" + (int) damage + " dmg)"), true);
            LeoEnchantsMod.LOGGER.info("Player {} launched obsidian strike to {} ({} blocks)", 
                player.getName().getString(), targetPos, (int) distance);
        }
        
        return ActionResult.SUCCESS;
    }
    
    /**
     * Get the target position from a hit result
     */
    private static Vec3d getTargetPosition(HitResult hitResult) {
        if (hitResult == null) return null;
        
        return switch (hitResult.getType()) {
            case BLOCK -> {
                BlockHitResult blockHit = (BlockHitResult) hitResult;
                BlockPos pos = blockHit.getBlockPos();
                // Return the position above the block for bridges
                yield new Vec3d(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
            }
            case ENTITY -> {
                EntityHitResult entityHit = (EntityHitResult) hitResult;
                yield entityHit.getEntity().getPos();
            }
            case MISS -> null;
        };
    }
    
    /**
     * Perform a raycast from the player's view
     */
    private static HitResult raycast(PlayerEntity player, double maxDistance) {
        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVector();
        Vec3d end = start.add(direction.multiply(maxDistance));
        
        // First check for entity hits
        EntityHitResult entityHit = raycastEntities(player, start, end);
        
        // Then check for block hits
        BlockHitResult blockHit = player.getWorld().raycast(new RaycastContext(
            start, end,
            RaycastContext.ShapeType.OUTLINE,
            RaycastContext.FluidHandling.NONE,
            player
        ));
        
        // Return the closer hit
        if (entityHit != null) {
            double entityDist = start.squaredDistanceTo(entityHit.getPos());
            double blockDist = start.squaredDistanceTo(blockHit.getPos());
            
            if (entityDist < blockDist) {
                return entityHit;
            }
        }
        
        return blockHit;
    }
    
    /**
     * Raycast for entities
     */
    private static EntityHitResult raycastEntities(PlayerEntity player, Vec3d start, Vec3d end) {
        double closestDistance = Double.MAX_VALUE;
        Entity closestEntity = null;
        Vec3d closestHitPos = null;
        
        for (Entity entity : player.getWorld().getOtherEntities(player, 
                player.getBoundingBox().expand(MAX_RAYCAST_DISTANCE))) {
            
            if (!entity.isAttackable()) continue;
            
            // Check if the ray intersects this entity
            var box = entity.getBoundingBox().expand(0.3);
            var optional = box.raycast(start, end);
            
            if (optional.isPresent()) {
                Vec3d hitPos = optional.get();
                double distance = start.squaredDistanceTo(hitPos);
                
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestEntity = entity;
                    closestHitPos = hitPos;
                }
            }
        }
        
        if (closestEntity != null) {
            return new EntityHitResult(closestEntity, closestHitPos);
        }
        
        return null;
    }
}

