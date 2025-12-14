package com.leo.enchants.logic;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the Hookshot enchantment logic for instant grappling functionality.
 * When a player uses a fishing rod with Hookshot enchantment:
 * - Hook attaches to blocks instantly (very fast projectile)
 * - Player is pulled to the block on retrieval
 * - Player is held near the block for 2 seconds
 * - Safe positioning to avoid suffocation
 */
public class HookshotHandler {

    // Hold duration in ticks (2 seconds = 40 ticks)
    private static final int HOLD_DURATION_TICKS = 40;
    
    // How long to actively pull toward target (in ticks) before starting hold
    private static final int PULL_PHASE_TICKS = 30;
    
    // Track players being held at hookshot positions
    private static final Map<UUID, HookshotHoldData> activeHolds = new HashMap<>();

    /**
     * Data class to track hookshot hold state
     */
    public static class HookshotHoldData {
        public final Vec3d holdPosition;
        public final long pullEndTime;    // When to stop strong pull
        public final long holdEndTime;    // When to release completely
        public final BlockPos attachedBlock;
        public boolean reachedTarget;     // Has player reached the target?
        
        public HookshotHoldData(Vec3d holdPosition, long pullEndTime, long holdEndTime, BlockPos attachedBlock) {
            this.holdPosition = holdPosition;
            this.pullEndTime = pullEndTime;
            this.holdEndTime = holdEndTime;
            this.attachedBlock = attachedBlock;
            this.reachedTarget = false;
        }
    }
    
    /**
     * Pull the player toward the hookshot target position.
     * 
     * @param player The player to pull
     * @param targetPos The position to pull toward (block position)
     * @param attachedBlock The block the hook attached to
     */
    public static void pullPlayerToBlock(PlayerEntity player, Vec3d targetPos, BlockPos attachedBlock) {
        Vec3d playerPos = player.getPos();
        
        // Calculate a safe position adjacent to the block (not inside it)
        Vec3d safePosition = findSafePosition(player, targetPos, attachedBlock);
        
        // Grant fall damage immunity
        if (player instanceof ServerPlayerEntity serverPlayer) {
            ServerWorld serverWorld = (ServerWorld) serverPlayer.getWorld();
            long currentTime = serverWorld.getTime();
            FallDamageImmunity.grantImmunity(serverPlayer, currentTime);
            
            // Register the hold - pull phase + hold phase
            long pullEndTime = currentTime + PULL_PHASE_TICKS;
            long holdEndTime = pullEndTime + HOLD_DURATION_TICKS;
            activeHolds.put(player.getUuid(), new HookshotHoldData(safePosition, pullEndTime, holdEndTime, attachedBlock));
        }
        
        // Reset fall distance
        player.fallDistance = 0;
        
        // Play grappling sound
        player.getWorld().playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            SoundEvents.ENTITY_FISHING_BOBBER_RETRIEVE,
            SoundCategory.PLAYERS,
            1.0f,
            2.0f // Higher pitch for hookshot
        );
        
        // Spawn particles along the path
        if (player.getWorld() instanceof ServerWorld serverWorld) {
            spawnHookshotParticles(serverWorld, playerPos, targetPos);
        }
    }
    
    /**
     * Find position right at the block surface.
     * This is where the player will be pulled to and held.
     */
    private static Vec3d findSafePosition(PlayerEntity player, Vec3d targetPos, BlockPos attachedBlock) {
        Vec3d playerPos = player.getPos();
        Vec3d blockCenter = Vec3d.ofCenter(attachedBlock);
        
        // Direction from player to block
        Vec3d toBlock = blockCenter.subtract(playerPos).normalize();
        
        // Position right at the block surface
        // Block surface is 0.5 from center, player needs ~0.4 clearance
        // So 0.9 from block center puts player right against the surface
        Vec3d safePos = blockCenter.subtract(toBlock.multiply(0.9));
        
        return safePos;
    }
    
    /**
     * Tick the hookshot hold system.
     * Should be called every server tick.
     */
    public static void tickHolds(ServerWorld world) {
        long currentTime = world.getTime();
        
        Iterator<Map.Entry<UUID, HookshotHoldData>> iterator = activeHolds.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, HookshotHoldData> entry = iterator.next();
            UUID playerId = entry.getKey();
            HookshotHoldData data = entry.getValue();
            
            // Find the player
            PlayerEntity player = world.getPlayerByUuid(playerId);
            if (player == null) {
                iterator.remove();
                continue;
            }
            
            // Check if hold has expired
            if (currentTime >= data.holdEndTime) {
                iterator.remove();
                continue;
            }
            
            double distanceToHold = player.getPos().distanceTo(data.holdPosition);
            
            // Pull phase - actively pull player toward target
            if (currentTime < data.pullEndTime) {
                applyPullEffect(player, data, distanceToHold);
            } else {
                // Hold phase - keep player stuck to wall
                applyHoldEffect(player, data, distanceToHold);
            }
            
            // Reset fall distance during entire hookshot
            player.fallDistance = 0;
            
            // Keep granting fall damage immunity
            FallDamageImmunity.grantImmunity(player, currentTime);
        }
    }
    
    /**
     * Apply strong pull effect to bring player to target.
     */
    private static void applyPullEffect(PlayerEntity player, HookshotHoldData data, double distance) {
        Vec3d playerPos = player.getPos();
        Vec3d holdPos = data.holdPosition;
        
        // Calculate direction to target
        Vec3d pullDir = holdPos.subtract(playerPos).normalize();
        
        // Very strong pull - get to block fast!
        double pullSpeed;
        if (distance > 5.0) {
            pullSpeed = 3.5; // Very fast when far
        } else if (distance > 1.5) {
            pullSpeed = 2.5; // Fast when medium distance
        } else {
            pullSpeed = 1.5; // Still fast when close
            data.reachedTarget = true;
        }
        
        // Set velocity directly toward target
        Vec3d newVelocity = pullDir.multiply(pullSpeed);
        player.setVelocity(newVelocity);
        player.velocityModified = true;
        
        // Check for suffocation during pull
        preventSuffocation(player, data);
    }
    
    /**
     * Apply hold effect to keep player stuck at the wall.
     */
    private static void applyHoldEffect(PlayerEntity player, HookshotHoldData data, double distance) {
        Vec3d holdPos = data.holdPosition;
        
        // If player is close enough, LOCK them to the position
        if (distance < 1.5) {
            // Teleport player to hold position to stick them to wall
            player.setPosition(holdPos.x, holdPos.y, holdPos.z);
            
            // Zero out velocity to prevent drifting
            player.setVelocity(0, 0, 0);
            player.velocityModified = true;
            
            // Check for suffocation and adjust if needed
            preventSuffocation(player, data);
        } else if (distance < 5.0) {
            // Player drifted, pull them back strongly
            Vec3d playerPos = player.getPos();
            Vec3d pullDir = holdPos.subtract(playerPos).normalize();
            double pullStrength = Math.min(distance * 0.8, 2.0);
            
            Vec3d newVelocity = pullDir.multiply(pullStrength);
            player.setVelocity(newVelocity);
            player.velocityModified = true;
        }
        // If player is too far (>5 blocks), they've escaped somehow, let them go
    }
    
    /**
     * Prevent player from suffocating in blocks.
     */
    private static void preventSuffocation(PlayerEntity player, HookshotHoldData data) {
        BlockPos playerBlockPos = player.getBlockPos();
        BlockPos headPos = playerBlockPos.up();
        
        boolean feetInBlock = !player.getWorld().getBlockState(playerBlockPos).isAir() && 
                              player.getWorld().getBlockState(playerBlockPos).isSolidBlock(player.getWorld(), playerBlockPos);
        boolean headInBlock = !player.getWorld().getBlockState(headPos).isAir() && 
                              player.getWorld().getBlockState(headPos).isSolidBlock(player.getWorld(), headPos);
        
        if (feetInBlock || headInBlock) {
            // Push player out of the block
            Vec3d blockCenter = Vec3d.ofCenter(data.attachedBlock);
            Vec3d playerPos = player.getPos();
            
            Vec3d pushDir = playerPos.subtract(blockCenter);
            if (pushDir.horizontalLength() < 0.1) {
                // If directly above/below, push based on original approach direction
                pushDir = data.holdPosition.subtract(blockCenter).normalize().multiply(-1);
            } else {
                pushDir = new Vec3d(pushDir.x, 0, pushDir.z).normalize();
            }
            
            // Move player out
            player.setPosition(player.getX() + pushDir.x * 0.5, 
                             player.getY() + 0.1, 
                             player.getZ() + pushDir.z * 0.5);
        }
    }
    
    /**
     * Check if a player is currently in a hookshot hold.
     */
    public static boolean isInHold(UUID playerId) {
        return activeHolds.containsKey(playerId);
    }
    
    /**
     * Cancel a player's hookshot hold.
     */
    public static void cancelHold(UUID playerId) {
        activeHolds.remove(playerId);
    }
    
    /**
     * Spawn particles along the hookshot path.
     */
    private static void spawnHookshotParticles(ServerWorld world, Vec3d start, Vec3d end) {
        Vec3d direction = end.subtract(start);
        double distance = direction.length();
        Vec3d step = direction.normalize().multiply(0.3);
        
        int particleCount = (int) Math.min(distance * 3, 30);
        Vec3d currentPos = start;
        
        for (int i = 0; i < particleCount; i++) {
            world.spawnParticles(
                ParticleTypes.CRIT,
                currentPos.x,
                currentPos.y,
                currentPos.z,
                1,
                0.05, 0.05, 0.05,
                0.01
            );
            currentPos = currentPos.add(step);
        }
    }
    
    /**
     * Clean up data for a disconnected player.
     */
    public static void removePlayer(UUID playerId) {
        activeHolds.remove(playerId);
    }
}
