package com.leo.enchants.logic;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/**
 * Handles the Grab enchantment logic for grappling hook functionality.
 * When a player retracts a fishing rod with Grab enchantment, they are
 * pulled toward the bobber's position.
 */
public class GrabHandler {

    // Maximum pull velocity to prevent excessive speed
    private static final double MAX_VELOCITY = 2.5;
    
    // Base pull strength multiplier
    private static final double PULL_STRENGTH = 0.15;
    
    // Vertical boost to help clear obstacles
    private static final double VERTICAL_BOOST = 0.3;

    /**
     * Pull the player toward the bobber's position.
     * 
     * @param player The player to pull
     * @param bobberPos The position of the fishing bobber
     * @param distance The distance between player and bobber
     */
    public static void pullPlayerToBobber(PlayerEntity player, Vec3d bobberPos, double distance) {
        Vec3d playerPos = player.getPos().add(0, player.getEyeHeight(player.getPose()) / 2, 0);
        
        // Calculate direction from player to bobber
        Vec3d direction = bobberPos.subtract(playerPos).normalize();
        
        // Calculate pull velocity based on distance (stronger pull for longer distances)
        double pullMultiplier = Math.min(distance * PULL_STRENGTH, MAX_VELOCITY);
        
        // Calculate velocity components
        double velX = direction.x * pullMultiplier;
        double velY = direction.y * pullMultiplier + VERTICAL_BOOST; // Add vertical boost
        double velZ = direction.z * pullMultiplier;
        
        // Clamp the total velocity
        Vec3d newVelocity = new Vec3d(velX, velY, velZ);
        if (newVelocity.length() > MAX_VELOCITY) {
            newVelocity = newVelocity.normalize().multiply(MAX_VELOCITY);
        }
        
        // Set player velocity
        player.setVelocity(newVelocity);
        player.velocityModified = true;
        
        // Reset fall distance to prevent fall damage from the launch
        player.fallDistance = 0;
        
        // Grant temporary fall damage immunity
        if (player instanceof ServerPlayerEntity serverPlayer) {
            ServerWorld serverWorld = (ServerWorld) serverPlayer.getWorld();
            long currentTime = serverWorld.getTime();
            FallDamageImmunity.grantImmunity(serverPlayer, currentTime);
        }
        
        // Play grappling sound
        player.getWorld().playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            SoundEvents.ENTITY_FISHING_BOBBER_RETRIEVE,
            SoundCategory.PLAYERS,
            1.0f,
            1.5f
        );
        
        // Spawn particles along the path (server-side)
        if (player.getWorld() instanceof ServerWorld serverWorld) {
            spawnGrappleParticles(serverWorld, playerPos, bobberPos);
        }
    }
    
    /**
     * Spawn particles along the grapple path to visualize the pull.
     */
    private static void spawnGrappleParticles(ServerWorld world, Vec3d start, Vec3d end) {
        Vec3d direction = end.subtract(start);
        double distance = direction.length();
        Vec3d step = direction.normalize().multiply(0.5);
        
        int particleCount = (int) Math.min(distance * 2, 20);
        Vec3d currentPos = start;
        
        for (int i = 0; i < particleCount; i++) {
            world.spawnParticles(
                ParticleTypes.FISHING,
                currentPos.x,
                currentPos.y,
                currentPos.z,
                1,
                0.1, 0.1, 0.1,
                0.02
            );
            currentPos = currentPos.add(step);
        }
    }
}

