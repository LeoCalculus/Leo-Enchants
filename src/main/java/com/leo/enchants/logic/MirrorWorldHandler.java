package com.leo.enchants.logic;

import com.leo.enchants.LeoEnchantsMod;
import com.leo.enchants.entity.MirrorBarrierEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles the Mirror World mechanic.
 * 
 * When activated:
 * - Snapshots all blocks in a 50 block radius
 * - Tracks player inventories to prevent item duplication
 * - After 60 seconds, restores the original world state
 * - Maximum 5 mirror worlds can exist simultaneously per dimension
 * - Mirror Barrier item used to activate is consumed (not returned)
 * - Other Mirror Barrier items in inventory ARE preserved
 */
public class MirrorWorldHandler {
    
    // Maximum number of mirror worlds per dimension
    private static final int MAX_MIRROR_WORLDS = 5;
    
    // Duration of mirror world in ticks (60 seconds = 1200 ticks)
    private static final int MIRROR_DURATION_TICKS = 1200;
    
    // Block radius for calculations (50 blocks)
    private static final int BLOCK_RADIUS = 50;
    
    // Vertical range to snapshot (centered on player Y)
    private static final int VERTICAL_RANGE = 30;
    
    // Active mirror worlds by dimension
    private static final Map<World, List<MirrorWorld>> activeMirrorWorlds = new ConcurrentHashMap<>();
    
    // Player inventory snapshots when entering mirror worlds
    private static final Map<UUID, InventorySnapshot> playerInventorySnapshots = new ConcurrentHashMap<>();
    
    // Players currently in mirror worlds
    private static final Map<UUID, UUID> playersInMirrorWorlds = new ConcurrentHashMap<>();
    
    /**
     * Represents a single active mirror world
     */
    public static class MirrorWorld {
        public final UUID id;
        public final ServerWorld world;
        public final BlockPos center;
        public final long startTime;
        public final int minY;
        public final int maxY;
        public final Map<BlockPos, BlockSnapshot> blockSnapshots;
        public final Set<UUID> affectedPlayers;
        public MirrorBarrierEntity floatingItem;
        
        public MirrorWorld(UUID id, ServerWorld world, BlockPos center, long startTime, int minY, int maxY) {
            this.id = id;
            this.world = world;
            this.center = center;
            this.startTime = startTime;
            this.minY = minY;
            this.maxY = maxY;
            this.blockSnapshots = new HashMap<>();
            this.affectedPlayers = new HashSet<>();
        }
        
        public boolean isExpired(long currentTime) {
            return currentTime - startTime >= MIRROR_DURATION_TICKS;
        }
        
        public int getRemainingSeconds(long currentTime) {
            long remaining = MIRROR_DURATION_TICKS - (currentTime - startTime);
            return Math.max(0, (int) (remaining / 20));
        }
        
        public boolean isInRange(BlockPos pos) {
            double dx = pos.getX() - center.getX();
            double dz = pos.getZ() - center.getZ();
            return Math.sqrt(dx * dx + dz * dz) <= BLOCK_RADIUS;
        }
        
        public boolean isInRange(Vec3d pos) {
            double dx = pos.x - center.getX();
            double dz = pos.z - center.getZ();
            return Math.sqrt(dx * dx + dz * dz) <= BLOCK_RADIUS;
        }
    }
    
    /**
     * Snapshot of a single block
     */
    public static class BlockSnapshot {
        public final BlockState state;
        
        public BlockSnapshot(BlockState state) {
            this.state = state;
        }
    }
    
    /**
     * Snapshot of player inventory using slot-based access
     * ALL items are saved including Mirror Barrier items (the used one is already consumed before snapshot)
     */
    public static class InventorySnapshot {
        public final UUID playerId;
        public final UUID mirrorWorldId;
        public final Map<Integer, ItemStack> inventoryContents;
        
        private static final int INVENTORY_SIZE = 41;
        
        public InventorySnapshot(ServerPlayerEntity player, UUID mirrorWorldId) {
            this.playerId = player.getUuid();
            this.mirrorWorldId = mirrorWorldId;
            this.inventoryContents = new HashMap<>();
            
            // Snapshot ALL slots - the used Mirror Barrier is already consumed before this
            for (int i = 0; i < INVENTORY_SIZE; i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (!stack.isEmpty()) {
                    inventoryContents.put(i, stack.copy());
                }
            }
        }
        
        public void restore(ServerPlayerEntity player) {
            player.getInventory().clear();
            
            for (Map.Entry<Integer, ItemStack> entry : inventoryContents.entrySet()) {
                player.getInventory().setStack(entry.getKey(), entry.getValue().copy());
            }
            
            player.getInventory().markDirty();
            player.currentScreenHandler.sendContentUpdates();
        }
    }
    
    public static boolean canCreateMirrorWorld(ServerWorld world) {
        List<MirrorWorld> worldMirrors = activeMirrorWorlds.get(world);
        if (worldMirrors == null) {
            return true;
        }
        return worldMirrors.size() < MAX_MIRROR_WORLDS;
    }
    
    public static boolean isInMirrorWorld(ServerPlayerEntity player) {
        return playersInMirrorWorlds.containsKey(player.getUuid());
    }
    
    public static boolean mirrorWorldExists(UUID mirrorWorldId) {
        for (List<MirrorWorld> worlds : activeMirrorWorlds.values()) {
            for (MirrorWorld world : worlds) {
                if (world.id.equals(mirrorWorldId)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Create a new mirror world centered on the player's position
     */
    public static boolean createMirrorWorld(ServerWorld world, ServerPlayerEntity activator, ItemStack item) {
        if (!canCreateMirrorWorld(world)) {
            return false;
        }
        
        UUID mirrorId = UUID.randomUUID();
        BlockPos center = activator.getBlockPos();
        long startTime = world.getTime();
        
        // Calculate Y range centered on player
        int minY = Math.max(world.getBottomY(), center.getY() - VERTICAL_RANGE);
        int maxY = Math.min(world.getTopYInclusive(), center.getY() + VERTICAL_RANGE);
        
        MirrorWorld mirrorWorld = new MirrorWorld(mirrorId, world, center, startTime, minY, maxY);
        
        LeoEnchantsMod.LOGGER.info("Creating mirror world {} at {} with {} block radius", 
            mirrorId, center, BLOCK_RADIUS);
        
        // Snapshot all blocks in range
        snapshotBlocks(mirrorWorld);
        
        // Add to active mirror worlds
        activeMirrorWorlds.computeIfAbsent(world, k -> new ArrayList<>()).add(mirrorWorld);
        
        // Snapshot the activator's inventory (item is already consumed at this point)
        snapshotPlayerInventory(activator, mirrorId);
        mirrorWorld.affectedPlayers.add(activator.getUuid());
        
        // Also snapshot all players currently in the radius
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!player.equals(activator) && mirrorWorld.isInRange(player.getPos())) {
                snapshotPlayerInventory(player, mirrorId);
                mirrorWorld.affectedPlayers.add(player.getUuid());
            }
        }
        
        // Create the floating item entity
        Vec3d floatPos = new Vec3d(center.getX() + 0.5, center.getY() + 2.0, center.getZ() + 0.5);
        MirrorBarrierEntity floatingEntity = new MirrorBarrierEntity(world, floatPos, item, mirrorId);
        world.spawnEntity(floatingEntity);
        mirrorWorld.floatingItem = floatingEntity;
        
        // Play activation sound
        world.playSound(null, center, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 1.0f, 1.5f);
        
        // Spawn visual effect
        spawnActivationParticles(world, center);
        
        return true;
    }
    
    /**
     * Snapshot all blocks within the mirror world radius (limited Y range for performance)
     */
    private static void snapshotBlocks(MirrorWorld mirrorWorld) {
        ServerWorld world = mirrorWorld.world;
        BlockPos center = mirrorWorld.center;
        
        int minX = center.getX() - BLOCK_RADIUS;
        int maxX = center.getX() + BLOCK_RADIUS;
        int minZ = center.getZ() - BLOCK_RADIUS;
        int maxZ = center.getZ() + BLOCK_RADIUS;
        
        // Use limited Y range for faster processing
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                double dx = x - center.getX();
                double dz = z - center.getZ();
                if (dx * dx + dz * dz > BLOCK_RADIUS * BLOCK_RADIUS) {
                    continue;
                }
                
                for (int y = mirrorWorld.minY; y <= mirrorWorld.maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    
                    if (!state.isAir()) {
                        mirrorWorld.blockSnapshots.put(pos.toImmutable(), new BlockSnapshot(state));
                    }
                }
            }
        }
        
        LeoEnchantsMod.LOGGER.info("Snapshotted {} blocks for mirror world {}", 
            mirrorWorld.blockSnapshots.size(), mirrorWorld.id);
    }
    
    private static void snapshotPlayerInventory(ServerPlayerEntity player, UUID mirrorWorldId) {
        InventorySnapshot snapshot = new InventorySnapshot(player, mirrorWorldId);
        playerInventorySnapshots.put(player.getUuid(), snapshot);
        playersInMirrorWorlds.put(player.getUuid(), mirrorWorldId);
        
        // Action bar message
        player.sendMessage(Text.literal("§b[Mirror Barrier] §7Inventory recorded"), true);
    }
    
    /**
     * Handle a player leaving the mirror zone - restore their inventory
     */
    private static void handlePlayerLeaveMirrorZone(ServerPlayerEntity player, MirrorWorld mirrorWorld) {
        UUID playerId = player.getUuid();
        
        // Remove from affected players
        mirrorWorld.affectedPlayers.remove(playerId);
        
        // Restore inventory
        InventorySnapshot invSnapshot = playerInventorySnapshots.remove(playerId);
        playersInMirrorWorlds.remove(playerId);
        
        if (invSnapshot != null) {
            invSnapshot.restore(player);
            player.sendMessage(Text.literal("§b[Mirror Barrier] §7Left mirror zone - inventory restored"), true);
        }
    }
    
    /**
     * Restore the world state when a mirror world expires
     */
    private static void restoreMirrorWorld(MirrorWorld mirrorWorld) {
        ServerWorld world = mirrorWorld.world;
        
        LeoEnchantsMod.LOGGER.info("Restoring mirror world {} with {} blocks", 
            mirrorWorld.id, mirrorWorld.blockSnapshots.size());
        
        BlockPos center = mirrorWorld.center;
        int minX = center.getX() - BLOCK_RADIUS;
        int maxX = center.getX() + BLOCK_RADIUS;
        int minZ = center.getZ() - BLOCK_RADIUS;
        int maxZ = center.getZ() + BLOCK_RADIUS;
        
        // Remove all item entities in the mirror zone to prevent duplication
        Box boundingBox = new Box(
            minX, mirrorWorld.minY, minZ,
            maxX + 1, mirrorWorld.maxY + 1, maxZ + 1
        );
        
        List<ItemEntity> itemsToRemove = world.getEntitiesByClass(ItemEntity.class, boundingBox, 
            item -> mirrorWorld.isInRange(item.getPos()));
        for (ItemEntity item : itemsToRemove) {
            item.discard();
        }
        
        // Clear blocks that shouldn't exist (blocks placed during mirror)
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                double dx = x - center.getX();
                double dz = z - center.getZ();
                if (dx * dx + dz * dz > BLOCK_RADIUS * BLOCK_RADIUS) {
                    continue;
                }
                
                for (int y = mirrorWorld.minY; y <= mirrorWorld.maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState currentState = world.getBlockState(pos);
                    
                    if (!mirrorWorld.blockSnapshots.containsKey(pos) && !currentState.isAir()) {
                        world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2 | 16);
                    }
                }
            }
        }
        
        // Restore all snapshotted blocks
        for (Map.Entry<BlockPos, BlockSnapshot> entry : mirrorWorld.blockSnapshots.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockSnapshot snapshot = entry.getValue();
            world.setBlockState(pos, snapshot.state, 2 | 16);
        }
        
        // Restore player inventories for players still in the zone
        for (UUID playerId : mirrorWorld.affectedPlayers) {
            InventorySnapshot invSnapshot = playerInventorySnapshots.remove(playerId);
            playersInMirrorWorlds.remove(playerId);
            
            if (invSnapshot != null) {
                ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(playerId);
                if (player != null) {
                    invSnapshot.restore(player);
                    player.sendMessage(Text.literal("§b[Mirror Barrier] §7Mirror collapsed - inventory restored"), true);
                }
            }
        }
        
        // Remove the floating item entity
        if (mirrorWorld.floatingItem != null) {
            mirrorWorld.floatingItem.discard();
        }
        
        // Play collapse sound
        world.playSound(null, mirrorWorld.center, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.0f, 0.5f);
        world.playSound(null, mirrorWorld.center, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 0.8f);
        
        // Spawn collapse particles
        spawnCollapseParticles(world, mirrorWorld.center);
    }
    
    private static void spawnActivationParticles(ServerWorld world, BlockPos center) {
        for (int i = 0; i < 50; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double radius = world.random.nextDouble() * 3;
            double x = center.getX() + 0.5 + Math.cos(angle) * radius;
            double y = center.getY() + 1 + world.random.nextDouble() * 2;
            double z = center.getZ() + 0.5 + Math.sin(angle) * radius;
            
            world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0.5, 0, 0.1);
        }
    }
    
    private static void spawnCollapseParticles(ServerWorld world, BlockPos center) {
        for (int i = 0; i < 100; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double radius = world.random.nextDouble() * 5;
            double x = center.getX() + 0.5 + Math.cos(angle) * radius;
            double y = center.getY() + 1 + world.random.nextDouble() * 3;
            double z = center.getZ() + 0.5 + Math.sin(angle) * radius;
            
            world.spawnParticles(ParticleTypes.DRAGON_BREATH, x, y, z, 1, 0, 0.2, 0, 0.02);
        }
    }
    
    /**
     * Tick all active mirror worlds
     */
    public static void tickMirrorWorlds(ServerWorld world) {
        List<MirrorWorld> worldMirrors = activeMirrorWorlds.get(world);
        if (worldMirrors == null || worldMirrors.isEmpty()) {
            return;
        }
        
        long currentTime = world.getTime();
        List<MirrorWorld> toRemove = new ArrayList<>();
        
        for (MirrorWorld mirrorWorld : worldMirrors) {
            if (mirrorWorld.isExpired(currentTime)) {
                restoreMirrorWorld(mirrorWorld);
                toRemove.add(mirrorWorld);
                continue;
            }
            
            // Check for players leaving the mirror zone
            List<UUID> playersToRemove = new ArrayList<>();
            for (UUID playerId : mirrorWorld.affectedPlayers) {
                ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(playerId);
                if (player != null && !mirrorWorld.isInRange(player.getPos())) {
                    playersToRemove.add(playerId);
                }
            }
            
            // Handle players who left the zone
            for (UUID playerId : playersToRemove) {
                ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(playerId);
                if (player != null) {
                    handlePlayerLeaveMirrorZone(player, mirrorWorld);
                }
            }
            
            // Track new players entering the mirror zone
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (mirrorWorld.isInRange(player.getPos()) && !mirrorWorld.affectedPlayers.contains(player.getUuid())) {
                    if (!playersInMirrorWorlds.containsKey(player.getUuid())) {
                        snapshotPlayerInventory(player, mirrorWorld.id);
                        mirrorWorld.affectedPlayers.add(player.getUuid());
                        player.sendMessage(Text.literal("§b[Mirror Barrier] §7Entered mirror world - §e" + 
                            mirrorWorld.getRemainingSeconds(currentTime) + "s §7remaining"), true);
                    }
                }
            }
            
            // Prevent items from being taken outside the mirror zone
            preventItemExfiltration(mirrorWorld);
            
            // Send periodic warnings (action bar)
            int remaining = mirrorWorld.getRemainingSeconds(currentTime);
            if (remaining == 30 || remaining == 10 || remaining == 5) {
                for (UUID playerId : mirrorWorld.affectedPlayers) {
                    ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(playerId);
                    if (player != null) {
                        player.sendMessage(Text.literal("§b[Mirror Barrier] §c" + remaining + "s §7until collapse!"), true);
                    }
                }
            }
            
            // Spawn boundary particles every 2 seconds
            if (currentTime % 40 == 0) {
                spawnBoundaryParticles(world, mirrorWorld);
            }
        }
        
        worldMirrors.removeAll(toRemove);
    }
    
    private static void preventItemExfiltration(MirrorWorld mirrorWorld) {
        ServerWorld world = mirrorWorld.world;
        double boundaryBuffer = 5.0;
        BlockPos center = mirrorWorld.center;
        
        Box checkBox = new Box(
            center.getX() - BLOCK_RADIUS - boundaryBuffer,
            mirrorWorld.minY,
            center.getZ() - BLOCK_RADIUS - boundaryBuffer,
            center.getX() + BLOCK_RADIUS + boundaryBuffer,
            mirrorWorld.maxY,
            center.getZ() + BLOCK_RADIUS + boundaryBuffer
        );
        
        List<ItemEntity> nearbyItems = world.getEntitiesByClass(ItemEntity.class, checkBox, item -> true);
        
        for (ItemEntity item : nearbyItems) {
            Vec3d itemPos = item.getPos();
            
            if (!mirrorWorld.isInRange(itemPos)) {
                Vec3d velocity = item.getVelocity();
                Vec3d previousPos = itemPos.subtract(velocity);
                
                if (mirrorWorld.isInRange(previousPos)) {
                    double dx = itemPos.x - center.getX();
                    double dz = itemPos.z - center.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    
                    if (dist > 0) {
                        double scale = (BLOCK_RADIUS - 2) / dist;
                        double newX = center.getX() + dx * scale;
                        double newZ = center.getZ() + dz * scale;
                        item.setPosition(newX, itemPos.y, newZ);
                        item.setVelocity(0, 0, 0);
                    }
                }
            }
        }
    }
    
    private static void spawnBoundaryParticles(ServerWorld world, MirrorWorld mirrorWorld) {
        BlockPos center = mirrorWorld.center;
        
        for (int i = 0; i < 24; i++) {
            double angle = (i / 24.0) * Math.PI * 2;
            double x = center.getX() + 0.5 + Math.cos(angle) * BLOCK_RADIUS;
            double z = center.getZ() + 0.5 + Math.sin(angle) * BLOCK_RADIUS;
            double y = center.getY() + 1 + world.random.nextDouble() * 5;
            
            world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0.5, 0, 0.01);
        }
    }
    
    public static int getMirrorWorldCount(World world) {
        List<MirrorWorld> worldMirrors = activeMirrorWorlds.get(world);
        return worldMirrors == null ? 0 : worldMirrors.size();
    }
    
    public static void cleanup() {
        for (List<MirrorWorld> worlds : activeMirrorWorlds.values()) {
            for (MirrorWorld mirrorWorld : worlds) {
                restoreMirrorWorld(mirrorWorld);
            }
        }
        activeMirrorWorlds.clear();
        playerInventorySnapshots.clear();
        playersInMirrorWorlds.clear();
    }
    
    public static MirrorWorld getMirrorWorldAt(World world, BlockPos pos) {
        List<MirrorWorld> worldMirrors = activeMirrorWorlds.get(world);
        if (worldMirrors == null) {
            return null;
        }
        
        for (MirrorWorld mirrorWorld : worldMirrors) {
            if (mirrorWorld.isInRange(pos)) {
                return mirrorWorld;
            }
        }
        return null;
    }
}
