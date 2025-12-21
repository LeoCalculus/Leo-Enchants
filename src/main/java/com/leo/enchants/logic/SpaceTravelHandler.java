package com.leo.enchants.logic;

import com.leo.enchants.entity.SpaceTravelPortalEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
        if (dimensionId.contains("nether")) {
            // In nether, search for safe space and create platform if needed
            return findNetherSafePosition(targetWorld, blockPos);
        } else if (dimensionId.contains("end")) {
            // In end, create/ensure obsidian platform and spawn there
            return createEndPlatform(targetWorld);
        } else {
            // Overworld - use heightmap and ensure safe landing
            int safeY = targetWorld.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (int) x, (int) z) + 1;
            BlockPos landingPos = new BlockPos((int) x, safeY - 1, (int) z);
            
            // If landing in water or on dangerous block, create platform
            if (isDangerousBlock(targetWorld, landingPos)) {
                createSafePlatform(targetWorld, landingPos.up());
                safeY = landingPos.up().getY() + 1;
            }
            
            return new Vec3d(x + 0.5, safeY, z + 0.5);
        }
    }
    
    /**
     * Find a safe position in the Nether, creating a platform if necessary
     */
    private static Vec3d findNetherSafePosition(ServerWorld world, BlockPos basePos) {
        int x = basePos.getX();
        int z = basePos.getZ();
        
        // Search for existing safe space (3x3 area check for robustness)
        for (int y = 32; y < 120; y++) {
            BlockPos checkPos = new BlockPos(x, y, z);
            if (isSafeNetherSpot(world, checkPos)) {
                return new Vec3d(x + 0.5, y, z + 0.5);
            }
        }
        
        // No safe spot found - create a platform
        // Find the best Y level (prefer around Y=70 for breathing room)
        int platformY = findBestNetherPlatformY(world, x, z);
        BlockPos platformPos = new BlockPos(x, platformY, z);
        
        createSafePlatform(world, platformPos);
        
        // Clear space above platform
        for (int dy = 0; dy < 3; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos clearPos = platformPos.add(dx, dy, dz);
                    if (!world.isAir(clearPos)) {
                        world.setBlockState(clearPos, Blocks.AIR.getDefaultState());
                    }
                }
            }
        }
        
        return new Vec3d(x + 0.5, platformY, z + 0.5);
    }
    
    /**
     * Check if a nether position is safe (2 high air space with solid non-dangerous floor)
     */
    private static boolean isSafeNetherSpot(ServerWorld world, BlockPos pos) {
        // Check for 2-block high air space
        if (!world.isAir(pos) || !world.isAir(pos.up())) {
            return false;
        }
        
        // Check floor is solid and not dangerous
        BlockPos floorPos = pos.down();
        BlockState floorState = world.getBlockState(floorPos);
        
        if (world.isAir(floorPos)) {
            return false;
        }
        
        // Check floor is not lava, fire, or other dangerous blocks
        if (isDangerousBlock(world, floorPos)) {
            return false;
        }
        
        // Check surrounding area isn't full of lava (3x3 check)
        int lavaCount = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos checkPos = floorPos.add(dx, 0, dz);
                if (isLavaOrFire(world, checkPos)) {
                    lavaCount++;
                }
            }
        }
        
        // Allow spot only if less than 3 adjacent lava blocks
        return lavaCount < 3;
    }
    
    /**
     * Find the best Y level to create a platform in the Nether
     */
    private static int findBestNetherPlatformY(ServerWorld world, int x, int z) {
        // Scan for a good spot (not in lava lake, preferably in open cave)
        int bestY = 70;
        int bestScore = -1000;
        
        for (int y = 40; y < 100; y++) {
            int score = 0;
            BlockPos pos = new BlockPos(x, y, z);
            
            // Prefer air space
            if (world.isAir(pos)) score += 10;
            if (world.isAir(pos.up())) score += 10;
            if (world.isAir(pos.up(2))) score += 5;
            
            // Penalize lava nearby
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dy = -1; dy <= 2; dy++) {
                        if (isLavaOrFire(world, pos.add(dx, dy, dz))) {
                            score -= 5;
                        }
                    }
                }
            }
            
            if (score > bestScore) {
                bestScore = score;
                bestY = y;
            }
        }
        
        return bestY;
    }
    
    /**
     * Check if a block is dangerous (lava, fire, magma, etc.)
     */
    private static boolean isDangerousBlock(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        
        return block == Blocks.LAVA || 
               block == Blocks.FIRE || 
               block == Blocks.SOUL_FIRE ||
               block == Blocks.MAGMA_BLOCK ||
               block == Blocks.CACTUS ||
               block == Blocks.SWEET_BERRY_BUSH ||
               block == Blocks.POWDER_SNOW ||
               block == Blocks.WITHER_ROSE;
    }
    
    /**
     * Check if a block is lava or fire
     */
    private static boolean isLavaOrFire(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        return block == Blocks.LAVA || block == Blocks.FIRE || block == Blocks.SOUL_FIRE;
    }
    
    /**
     * Create a safe 3x3 obsidian platform
     */
    private static void createSafePlatform(ServerWorld world, BlockPos center) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos platformPos = center.add(dx, -1, dz);
                world.setBlockState(platformPos, Blocks.OBSIDIAN.getDefaultState());
            }
        }
    }
    
    /**
     * Create the End spawn platform (obsidian platform at 100, 48, 0)
     */
    private static Vec3d createEndPlatform(ServerWorld world) {
        // Standard End spawn platform location
        int x = 100;
        int y = 49;
        int z = 0;
        
        BlockPos platformCenter = new BlockPos(x, y, z);
        
        // Create 5x5 obsidian platform
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos platformPos = platformCenter.add(dx, 0, dz);
                world.setBlockState(platformPos, Blocks.OBSIDIAN.getDefaultState());
            }
        }
        
        // Clear 3-high space above platform
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 1; dy <= 3; dy++) {
                    BlockPos clearPos = platformCenter.add(dx, dy, dz);
                    if (!world.isAir(clearPos)) {
                        world.setBlockState(clearPos, Blocks.AIR.getDefaultState());
                    }
                }
            }
        }
        
        return new Vec3d(x + 0.5, y + 1, z + 0.5);
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


