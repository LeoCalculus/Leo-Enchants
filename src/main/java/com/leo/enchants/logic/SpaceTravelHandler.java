package com.leo.enchants.logic;

import com.leo.enchants.entity.SpaceTravelPortalEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handler for Space Travel item logic.
 * Manages dimension selection, portal tracking, and teleportation.
 */
public class SpaceTravelHandler {
    
    // Track player's selected dimension
    private static final Map<UUID, String> playerDimensionSelection = new HashMap<>();
    
    // Track active portals per player
    private static final Map<UUID, SpaceTravelPortalEntity> activePortals = new HashMap<>();
    
    // Available dimensions
    private static final String[] DIMENSIONS = {
        "minecraft:overworld",
        "minecraft:the_nether",
        "minecraft:the_end"
    };
    
    /**
     * Get the dimension name suitable for display
     */
    public static String getDimensionDisplayName(String dimensionId) {
        return switch (dimensionId) {
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end" -> "The End";
            default -> dimensionId;
        };
    }
    
    /**
     * Cycle to the next dimension for the player
     */
    public static void cycleDimension(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        String currentDimension = playerDimensionSelection.getOrDefault(playerId, DIMENSIONS[0]);
        
        // Find current index and cycle to next
        int currentIndex = 0;
        for (int i = 0; i < DIMENSIONS.length; i++) {
            if (DIMENSIONS[i].equals(currentDimension)) {
                currentIndex = i;
                break;
            }
        }
        
        int nextIndex = (currentIndex + 1) % DIMENSIONS.length;
        String nextDimension = DIMENSIONS[nextIndex];
        
        // Skip current dimension
        String playerDimension = player.getWorld().getRegistryKey().getValue().toString();
        if (nextDimension.equals(playerDimension)) {
            nextIndex = (nextIndex + 1) % DIMENSIONS.length;
            nextDimension = DIMENSIONS[nextIndex];
        }
        
        playerDimensionSelection.put(playerId, nextDimension);
        
        String displayName = getDimensionDisplayName(nextDimension);
        player.sendMessage(Text.literal("§b[Space Travel] §7Target: §e" + displayName), true);
    }
    
    /**
     * Get the currently selected dimension for a player
     */
    public static String getSelectedDimension(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        String currentDimension = playerDimensionSelection.getOrDefault(playerId, DIMENSIONS[0]);
        
        // If current dimension is same as player's world, switch to another
        String playerDimension = player.getWorld().getRegistryKey().getValue().toString();
        if (currentDimension.equals(playerDimension)) {
            cycleDimension(player);
            return playerDimensionSelection.get(playerId);
        }
        
        return currentDimension;
    }
    
    /**
     * Check if a player has an active portal
     */
    public static boolean hasActivePortal(ServerPlayerEntity player) {
        SpaceTravelPortalEntity portal = activePortals.get(player.getUuid());
        return portal != null && !portal.isRemoved();
    }
    
    /**
     * Register an active portal for a player
     */
    public static void registerPortal(ServerPlayerEntity player, SpaceTravelPortalEntity portal) {
        activePortals.put(player.getUuid(), portal);
    }
    
    /**
     * Unregister a player's portal
     */
    public static void unregisterPortal(UUID playerId) {
        activePortals.remove(playerId);
    }
    
    /**
     * Teleport a player through the portal to the destination dimension
     */
    public static void teleportThroughPortal(ServerPlayerEntity player, SpaceTravelPortalEntity portal) {
        String targetDimensionId = portal.getTargetDimension();
        MinecraftServer server = player.getServer();
        if (server == null) return;
        
        // Get the target world
        RegistryKey<World> targetKey = RegistryKey.of(
            RegistryKeys.WORLD,
            Identifier.of(targetDimensionId)
        );
        
        ServerWorld targetWorld = server.getWorld(targetKey);
        if (targetWorld == null) {
            player.sendMessage(Text.literal("§c[Space Travel] §7Dimension not found!"), true);
            return;
        }
        
        // Calculate safe landing position in target dimension
        Vec3d targetPos = calculateSafeLandingPosition(player, targetWorld);
        
        // Create teleport target
        TeleportTarget teleportTarget = new TeleportTarget(
            targetWorld,
            targetPos,
            player.getVelocity(),
            player.getYaw(),
            player.getPitch(),
            TeleportTarget.NO_OP
        );
        
        // Teleport the player
        player.teleportTo(teleportTarget);
        
        String dimensionName = getDimensionDisplayName(targetDimensionId);
        player.sendMessage(Text.literal("§b[Space Travel] §7Arrived in §e" + dimensionName + "§7!"), true);
    }
    
    /**
     * Calculate a safe landing position in the target dimension
     */
    private static Vec3d calculateSafeLandingPosition(ServerPlayerEntity player, ServerWorld targetWorld) {
        // Use player's current X/Z coordinates, scaled appropriately
        double x = player.getX();
        double z = player.getZ();
        
        String dimensionId = targetWorld.getRegistryKey().getValue().toString();
        String sourceDimensionId = player.getWorld().getRegistryKey().getValue().toString();
        
        // Scale coordinates for nether travel
        if (sourceDimensionId.contains("overworld") && dimensionId.contains("nether")) {
            x /= 8;
            z /= 8;
        } else if (sourceDimensionId.contains("nether") && dimensionId.contains("overworld")) {
            x *= 8;
            z *= 8;
        }
        
        // Clamp coordinates to world border
        x = Math.max(-29999872, Math.min(29999872, x));
        z = Math.max(-29999872, Math.min(29999872, z));
        
        BlockPos blockPos = BlockPos.ofFloored(x, 64, z);
        
        // Find safe Y position
        int safeY;
        if (dimensionId.contains("nether")) {
            // In nether, search for open space
            safeY = findNetherSafeY(targetWorld, blockPos);
        } else if (dimensionId.contains("end")) {
            // In end, spawn on the obsidian platform
            safeY = 50;
            x = 100;
            z = 0;
        } else {
            // Overworld - use heightmap
            safeY = targetWorld.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (int) x, (int) z) + 1;
        }
        
        return new Vec3d(x, safeY, z);
    }
    
    /**
     * Find a safe Y coordinate in the nether
     */
    private static int findNetherSafeY(ServerWorld world, BlockPos basePos) {
        // Search from bottom to top for a 2-high air space
        for (int y = 32; y < 120; y++) {
            BlockPos checkPos = new BlockPos(basePos.getX(), y, basePos.getZ());
            if (world.isAir(checkPos) && world.isAir(checkPos.up())) {
                // Check there's solid ground below
                if (!world.isAir(checkPos.down())) {
                    return y;
                }
            }
        }
        return 64; // Fallback
    }
    
    /**
     * Get the dimension color for rendering
     * Returns RGB as an integer array
     */
    public static int[] getDimensionColor(String dimensionId) {
        if (dimensionId.contains("nether")) {
            return new int[]{255, 80, 40}; // Orange-red for nether
        } else if (dimensionId.contains("end")) {
            return new int[]{180, 100, 255}; // Purple for end
        } else {
            return new int[]{100, 200, 255}; // Light blue for overworld
        }
    }
}

