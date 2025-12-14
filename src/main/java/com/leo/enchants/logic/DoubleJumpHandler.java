package com.leo.enchants.logic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import com.leo.enchants.network.ModNetworking;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DoubleJumpHandler {
    private static final Identifier DOUBLE_JUMP_ID = Identifier.of("leo_enchants", "double_jump");
    
    // Track which players have used their double jump (reset on landing)
    private static final Map<UUID, Boolean> canDoubleJump = new HashMap<>();
    private static final Map<UUID, Boolean> wasOnGround = new HashMap<>();
    // Track if player has released jump key since leaving ground (prevents instant double jump)
    private static final Map<UUID, Boolean> hasReleasedJumpSinceLeavingGround = new HashMap<>();
    // Track fall damage immunity end time (client game time)
    private static final Map<UUID, Long> fallDamageImmunityEndTime = new HashMap<>();
    private static boolean jumpKeyWasPressed = false;
    
    // 3 seconds = 60 ticks
    private static final int IMMUNITY_DURATION_TICKS = 60;
    
    /**
     * Called every client tick to handle double jump logic
     */
    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        
        UUID playerId = player.getUuid();
        boolean isOnGround = player.isOnGround();
        boolean jumpKeyPressed = client.options.jumpKey.isPressed();
        long currentTime = player.getWorld().getTime();
        
        // Handle fall damage immunity
        tickFallDamageImmunity(player, playerId, isOnGround, currentTime);
        
        // When player is on ground, reset all states
        if (isOnGround) {
            canDoubleJump.put(playerId, true);
            hasReleasedJumpSinceLeavingGround.put(playerId, false);
        }
        
        // Track if player released jump key while in air
        // This prevents double jump from triggering during the initial jump
        if (!isOnGround && !jumpKeyPressed) {
            hasReleasedJumpSinceLeavingGround.put(playerId, true);
        }
        
        // Double jump logic:
        // - Must be in air (!isOnGround)
        // - Must be pressing jump now (jumpKeyPressed)
        // - Must NOT have been pressing jump last tick (!jumpKeyWasPressed) - detects new press
        // - Must have released jump at least once since leaving ground (prevents instant trigger)
        // - Must have double jump enchantment
        // - Must not have already used double jump this air time
        if (!isOnGround && jumpKeyPressed && !jumpKeyWasPressed) {
            if (hasReleasedJumpSinceLeavingGround.getOrDefault(playerId, false)
                    && canDoubleJump.getOrDefault(playerId, false)) {
                int level = getDoubleJumpLevel(player);
                if (level > 0) {
                    performDoubleJump(client, player, level, currentTime);
                    canDoubleJump.put(playerId, false);
                }
            }
        }
        
        jumpKeyWasPressed = jumpKeyPressed;
        wasOnGround.put(playerId, isOnGround);
    }
    
    /**
     * Handle fall damage immunity - reset fall distance while immune
     */
    private static void tickFallDamageImmunity(ClientPlayerEntity player, UUID playerId, boolean isOnGround, long currentTime) {
        Long endTime = fallDamageImmunityEndTime.get(playerId);
        
        if (endTime == null) {
            return;
        }
        
        // If player lands, cancel immunity
        if (isOnGround) {
            fallDamageImmunityEndTime.remove(playerId);
            return;
        }
        
        // If immunity expired, remove it
        if (currentTime >= endTime) {
            fallDamageImmunityEndTime.remove(playerId);
            return;
        }
        
        // Player has immunity - keep resetting fall distance
        player.fallDistance = 0;
    }
    
    /**
     * Get the level of double jump enchantment on player's boots
     * Returns 0 if not enchanted
     */
    private static int getDoubleJumpLevel(ClientPlayerEntity player) {
        ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);
        
        if (boots.isEmpty()) {
            return 0;
        }
        
        ItemEnchantmentsComponent enchantments = boots.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null) {
            return 0;
        }
        
        for (var entry : enchantments.getEnchantmentEntries()) {
            if (entry.getKey().matchesId(DOUBLE_JUMP_ID)) {
                return entry.getIntValue();
            }
        }
        
        return 0;
    }
    
    /**
     * Perform the double jump
     * @param level Enchantment level (1 = normal, 2 = horizontal boost)
     */
    private static void performDoubleJump(MinecraftClient client, ClientPlayerEntity player, int level, long currentTime) {
        Vec3d currentVelocity = player.getVelocity();
        Vec3d lookDirection = player.getRotationVector();
        
        double verticalVelocity;
        double horizontalBoost;
        
        if (level >= 2) {
            // Level 2: Slightly less vertical, but boost horizontal in look direction
            verticalVelocity = 0.4; // Less than level 1
            horizontalBoost = 0.6; // Horizontal boost in look direction
            
            // Calculate new horizontal velocity with boost in look direction
            double newVelX = currentVelocity.x + lookDirection.x * horizontalBoost;
            double newVelZ = currentVelocity.z + lookDirection.z * horizontalBoost;
            
            player.setVelocity(newVelX, verticalVelocity, newVelZ);
        } else {
            // Level 1: Normal double jump (mostly vertical)
            verticalVelocity = 0.5;
            player.setVelocity(currentVelocity.x, verticalVelocity, currentVelocity.z);
        }
        
        // Grant 3 seconds of fall damage immunity (client-side tracking)
        fallDamageImmunityEndTime.put(player.getUuid(), currentTime + IMMUNITY_DURATION_TICKS);
        player.fallDistance = 0;
        
        // Send packet to server to grant server-side fall damage immunity
        ModNetworking.sendDoubleJumpImmunityRequest();
        
        // Play a wind/swoosh sound
        player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.5f);
        
        // Spawn cloud particles at feet using the particle manager
        // More particles for level 2
        int particleCount = (level >= 2) ? 12 : 8;
        for (int i = 0; i < particleCount; i++) {
            double offsetX = (Math.random() - 0.5) * 0.5;
            double offsetZ = (Math.random() - 0.5) * 0.5;
            client.particleManager.addParticle(
                ParticleTypes.CLOUD,
                player.getX() + offsetX,
                player.getY(),
                player.getZ() + offsetZ,
                0.0, 0.1, 0.0
            );
        }
    }
    
    /**
     * Clean up player data (call when player disconnects)
     */
    public static void removePlayer(UUID playerId) {
        canDoubleJump.remove(playerId);
        wasOnGround.remove(playerId);
        hasReleasedJumpSinceLeavingGround.remove(playerId);
        fallDamageImmunityEndTime.remove(playerId);
    }
}
