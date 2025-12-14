package com.leo.enchants.logic;

import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks fall damage immunity for players.
 * Used by both Wither Impact and Double Jump enchantments.
 */
public class FallDamageImmunity {
    
    // Track when fall damage immunity expires (game time in ticks)
    private static final Map<UUID, Long> immunityEndTime = new HashMap<>();
    
    // 3 seconds = 60 ticks
    public static final int IMMUNITY_DURATION_TICKS = 60;
    
    /**
     * Grant fall damage immunity to a player for 3 seconds
     * @param player The player to grant immunity to
     * @param currentGameTime Current game time in ticks
     */
    public static void grantImmunity(PlayerEntity player, long currentGameTime) {
        immunityEndTime.put(player.getUuid(), currentGameTime + IMMUNITY_DURATION_TICKS);
        player.fallDistance = 0;
    }
    
    /**
     * Check if player has fall damage immunity and handle it
     * Should be called every tick for each player
     * @param player The player to check
     * @param currentGameTime Current game time in ticks
     * @return true if player has immunity (and fallDistance was reset)
     */
    public static boolean tickImmunity(PlayerEntity player, long currentGameTime) {
        UUID playerId = player.getUuid();
        Long endTime = immunityEndTime.get(playerId);
        
        if (endTime == null) {
            return false;
        }
        
        // If player is on ground, cancel immunity
        if (player.isOnGround()) {
            immunityEndTime.remove(playerId);
            return false;
        }
        
        // If immunity has expired, remove it
        if (currentGameTime >= endTime) {
            immunityEndTime.remove(playerId);
            return false;
        }
        
        // Player has immunity - reset fall distance
        player.fallDistance = 0;
        return true;
    }
    
    /**
     * Check if player currently has immunity (without ticking)
     */
    public static boolean hasImmunity(UUID playerId, long currentGameTime) {
        Long endTime = immunityEndTime.get(playerId);
        return endTime != null && currentGameTime < endTime;
    }
    
    /**
     * Remove player data (call when player disconnects)
     */
    public static void removePlayer(UUID playerId) {
        immunityEndTime.remove(playerId);
    }
}

