package com.leo.enchants.entity;

import com.leo.enchants.LeoEnchantsMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a temporary obsidian bridge that forms between two points.
 * The bridge appears block by block and disappears after 20 seconds.
 */
public class ObsidianBridgeEntity extends Entity {
    
    // Data tracker keys
    private static final TrackedData<Integer> LIFETIME = DataTracker.registerData(ObsidianBridgeEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> BUILDING_COMPLETE = DataTracker.registerData(ObsidianBridgeEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    
    @Override
    public boolean damage(ServerWorld world, net.minecraft.entity.damage.DamageSource source, float amount) {
        return false; // Cannot be damaged
    }
    
    // Constants
    private static final int MAX_LIFETIME_TICKS = 20 * 20; // 20 seconds
    private static final int BUILD_SPEED = 2; // Blocks per tick during construction
    
    // Instance variables
    private UUID ownerUUID;
    private Vec3d startPos;
    private Vec3d endPos;
    private List<BlockPos> bridgeBlocks = new ArrayList<>();
    private List<BlockState> originalStates = new ArrayList<>();
    private int currentBuildIndex = 0;
    private int ticksExisted = 0;
    
    public ObsidianBridgeEntity(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }
    
    public ObsidianBridgeEntity(World world, Vec3d start, Vec3d end, UUID ownerUUID) {
        this(ModEntities.OBSIDIAN_BRIDGE, world);
        this.startPos = start;
        this.endPos = end;
        this.ownerUUID = ownerUUID;
        this.setPosition(start.x, start.y, start.z);
        
        // Calculate all block positions for the bridge
        calculateBridgeBlocks();
    }
    
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(LIFETIME, 0);
        builder.add(BUILDING_COMPLETE, false);
    }
    
    /**
     * Calculate all block positions that make up the bridge using Bresenham's line algorithm
     */
    private void calculateBridgeBlocks() {
        if (startPos == null || endPos == null) return;
        
        bridgeBlocks.clear();
        originalStates.clear();
        
        int x1 = (int) Math.floor(startPos.x);
        int y1 = (int) Math.floor(startPos.y);
        int z1 = (int) Math.floor(startPos.z);
        int x2 = (int) Math.floor(endPos.x);
        int y2 = (int) Math.floor(endPos.y);
        int z2 = (int) Math.floor(endPos.z);
        
        // 3D Bresenham's line algorithm
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int dz = Math.abs(z2 - z1);
        
        int xs = x1 < x2 ? 1 : -1;
        int ys = y1 < y2 ? 1 : -1;
        int zs = z1 < z2 ? 1 : -1;
        
        int x = x1, y = y1, z = z1;
        
        // Driving axis is the one with the largest delta
        if (dx >= dy && dx >= dz) {
            int p1 = 2 * dy - dx;
            int p2 = 2 * dz - dx;
            while (x != x2) {
                addBridgeBlock(new BlockPos(x, y, z));
                x += xs;
                if (p1 >= 0) {
                    y += ys;
                    p1 -= 2 * dx;
                }
                if (p2 >= 0) {
                    z += zs;
                    p2 -= 2 * dx;
                }
                p1 += 2 * dy;
                p2 += 2 * dz;
            }
        } else if (dy >= dx && dy >= dz) {
            int p1 = 2 * dx - dy;
            int p2 = 2 * dz - dy;
            while (y != y2) {
                addBridgeBlock(new BlockPos(x, y, z));
                y += ys;
                if (p1 >= 0) {
                    x += xs;
                    p1 -= 2 * dy;
                }
                if (p2 >= 0) {
                    z += zs;
                    p2 -= 2 * dy;
                }
                p1 += 2 * dx;
                p2 += 2 * dz;
            }
        } else {
            int p1 = 2 * dy - dz;
            int p2 = 2 * dx - dz;
            while (z != z2) {
                addBridgeBlock(new BlockPos(x, y, z));
                z += zs;
                if (p1 >= 0) {
                    y += ys;
                    p1 -= 2 * dz;
                }
                if (p2 >= 0) {
                    x += xs;
                    p2 -= 2 * dz;
                }
                p1 += 2 * dy;
                p2 += 2 * dx;
            }
        }
        
        // Add the final block
        addBridgeBlock(new BlockPos(x2, y2, z2));
        
        LeoEnchantsMod.LOGGER.info("Obsidian Bridge: Calculated {} blocks", bridgeBlocks.size());
    }
    
    private void addBridgeBlock(BlockPos pos) {
        if (!bridgeBlocks.contains(pos)) {
            bridgeBlocks.add(pos);
            // Store original state for restoration (will be populated when placing)
            originalStates.add(null);
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        ticksExisted++;
        
        if (!getWorld().isClient) {
            ServerWorld serverWorld = (ServerWorld) getWorld();
            
            // Building phase
            if (!getBuildingComplete()) {
                for (int i = 0; i < BUILD_SPEED && currentBuildIndex < bridgeBlocks.size(); i++) {
                    BlockPos pos = bridgeBlocks.get(currentBuildIndex);
                    
                    // Store original state before placing
                    BlockState original = serverWorld.getBlockState(pos);
                    if (currentBuildIndex < originalStates.size()) {
                        originalStates.set(currentBuildIndex, original);
                    }
                    
                    // Only place if the block is air or replaceable
                    if (original.isAir() || original.isReplaceable()) {
                        serverWorld.setBlockState(pos, Blocks.OBSIDIAN.getDefaultState());
                        
                        // Spawn particles
                        serverWorld.spawnParticles(ParticleTypes.PORTAL,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            5, 0.3, 0.3, 0.3, 0.02);
                    }
                    
                    currentBuildIndex++;
                }
                
                // Check if building is complete
                if (currentBuildIndex >= bridgeBlocks.size()) {
                    setBuildingComplete(true);
                    serverWorld.playSound(null, getX(), getY(), getZ(),
                        SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.BLOCKS, 1.0f, 0.8f);
                }
            }
            
            // Lifetime countdown after building complete
            if (getBuildingComplete()) {
                setLifetime(getLifetime() + 1);
                
                // Spawn periodic particles along the bridge
                if (ticksExisted % 10 == 0) {
                    for (int i = 0; i < bridgeBlocks.size(); i += 5) {
                        BlockPos pos = bridgeBlocks.get(i);
                        serverWorld.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                            pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                            1, 0.2, 0.1, 0.2, 0.01);
                    }
                }
                
                // Despawn after lifetime
                if (getLifetime() >= MAX_LIFETIME_TICKS) {
                    removeBridge(serverWorld);
                    this.discard();
                }
            }
        }
    }
    
    /**
     * Remove all placed obsidian blocks
     */
    private void removeBridge(ServerWorld serverWorld) {
        // Remove blocks in reverse order for a cool effect
        for (int i = bridgeBlocks.size() - 1; i >= 0; i--) {
            BlockPos pos = bridgeBlocks.get(i);
            BlockState currentState = serverWorld.getBlockState(pos);
            
            // Only remove if it's still obsidian (player might have mined it)
            if (currentState.isOf(Blocks.OBSIDIAN)) {
                // Restore original state or set to air
                BlockState original = (i < originalStates.size()) ? originalStates.get(i) : null;
                if (original != null && !original.isOf(Blocks.OBSIDIAN)) {
                    serverWorld.setBlockState(pos, original);
                } else {
                    serverWorld.setBlockState(pos, Blocks.AIR.getDefaultState());
                }
                
                // Spawn disappear particles
                serverWorld.spawnParticles(ParticleTypes.PORTAL,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    10, 0.3, 0.3, 0.3, 0.05);
            }
        }
        
        serverWorld.playSound(null, getX(), getY(), getZ(),
            SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.BLOCKS, 1.0f, 0.5f);
        
        LeoEnchantsMod.LOGGER.info("Obsidian Bridge: Removed {} blocks", bridgeBlocks.size());
    }
    
    // Getters and Setters
    public int getLifetime() {
        return this.dataTracker.get(LIFETIME);
    }
    
    public void setLifetime(int lifetime) {
        this.dataTracker.set(LIFETIME, lifetime);
    }
    
    public boolean getBuildingComplete() {
        return this.dataTracker.get(BUILDING_COMPLETE);
    }
    
    public void setBuildingComplete(boolean complete) {
        this.dataTracker.set(BUILDING_COMPLETE, complete);
    }
    
    public UUID getOwnerUUID() {
        return ownerUUID;
    }
    
    public Vec3d getStartPos() {
        return startPos;
    }
    
    public Vec3d getEndPos() {
        return endPos;
    }
    
    public List<BlockPos> getBridgeBlocks() {
        return bridgeBlocks;
    }
    
    public int getCurrentBuildIndex() {
        return currentBuildIndex;
    }
    
    @Override
    public void readCustomData(ReadView readView) {
        setLifetime(readView.getInt("Lifetime", 0));
        setBuildingComplete(readView.getBoolean("BuildingComplete", false));
        currentBuildIndex = readView.getInt("CurrentBuildIndex", 0);
        ticksExisted = readView.getInt("TicksExisted", 0);
        
        String uuidStr = readView.getString("OwnerUUID", "");
        if (!uuidStr.isEmpty()) {
            try {
                ownerUUID = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException ignored) {}
        }
        
        // Read positions
        if (readView.contains("StartX")) {
            startPos = new Vec3d(
                readView.getDouble("StartX", 0),
                readView.getDouble("StartY", 0),
                readView.getDouble("StartZ", 0)
            );
        }
        if (readView.contains("EndX")) {
            endPos = new Vec3d(
                readView.getDouble("EndX", 0),
                readView.getDouble("EndY", 0),
                readView.getDouble("EndZ", 0)
            );
        }
        
        // Recalculate bridge blocks
        if (startPos != null && endPos != null) {
            calculateBridgeBlocks();
        }
    }
    
    @Override
    public void writeCustomData(WriteView writeView) {
        writeView.putInt("Lifetime", getLifetime());
        writeView.putBoolean("BuildingComplete", getBuildingComplete());
        writeView.putInt("CurrentBuildIndex", currentBuildIndex);
        writeView.putInt("TicksExisted", ticksExisted);
        
        if (ownerUUID != null) {
            writeView.putString("OwnerUUID", ownerUUID.toString());
        }
        
        if (startPos != null) {
            writeView.putDouble("StartX", startPos.x);
            writeView.putDouble("StartY", startPos.y);
            writeView.putDouble("StartZ", startPos.z);
        }
        if (endPos != null) {
            writeView.putDouble("EndX", endPos.x);
            writeView.putDouble("EndY", endPos.y);
            writeView.putDouble("EndZ", endPos.z);
        }
    }
}

