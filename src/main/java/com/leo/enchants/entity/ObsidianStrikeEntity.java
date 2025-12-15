package com.leo.enchants.entity;

import com.leo.enchants.LeoEnchantsMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Obsidian Strike Entity - A projectile obsidian stripe that launches from the player
 * toward a target entity or block. Entities can stand on the stripe.
 * 
 * Damage scales with distance: further = more damage.
 * The stripe disappears after 10 seconds.
 */
public class ObsidianStrikeEntity extends Entity {
    
    // Data tracker keys
    private static final TrackedData<Integer> LIFETIME = DataTracker.registerData(ObsidianStrikeEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> STRIKE_COMPLETE = DataTracker.registerData(ObsidianStrikeEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Float> TOTAL_DISTANCE = DataTracker.registerData(ObsidianStrikeEntity.class, TrackedDataHandlerRegistry.FLOAT);
    
    // Constants
    private static final int MAX_LIFETIME_TICKS = 3 * 20; // ~3 seconds (3x faster than build mode's 20s)
    private static final int STRIKE_SPEED = 5; // Blocks per tick during strike animation
    private static final float BASE_DAMAGE = 5.0f;
    private static final float DAMAGE_PER_BLOCK = 0.5f; // Additional damage per block of distance
    private static final float MAX_DAMAGE = 50.0f;
    
    // Instance variables
    private UUID ownerUUID;
    private Vec3d startPos;
    private Vec3d endPos;
    private List<BlockPos> strikeBlocks = new ArrayList<>();
    private List<BlockState> originalStates = new ArrayList<>();
    private int currentStrikeIndex = 0;
    private int ticksExisted = 0;
    private Set<UUID> damagedEntities = new HashSet<>();
    private Entity targetEntity;
    
    public ObsidianStrikeEntity(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }
    
    public ObsidianStrikeEntity(World world, Vec3d start, Vec3d end, UUID ownerUUID, Entity target) {
        this(ModEntities.OBSIDIAN_STRIKE, world);
        this.startPos = start;
        this.endPos = end;
        this.ownerUUID = ownerUUID;
        this.targetEntity = target;
        this.setPosition(start.x, start.y, start.z);
        
        // Calculate distance for damage scaling
        float distance = (float) start.distanceTo(end);
        setTotalDistance(distance);
        
        // Calculate all block positions for the strike
        calculateStrikeBlocks();
    }
    
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(LIFETIME, 0);
        builder.add(STRIKE_COMPLETE, false);
        builder.add(TOTAL_DISTANCE, 0.0f);
    }
    
    /**
     * Calculate all block positions that make up the strike using 3D line algorithm
     */
    private void calculateStrikeBlocks() {
        if (startPos == null || endPos == null) return;
        
        strikeBlocks.clear();
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
        
        if (dx >= dy && dx >= dz) {
            int p1 = 2 * dy - dx;
            int p2 = 2 * dz - dx;
            while (x != x2) {
                addStrikeBlock(new BlockPos(x, y, z));
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
                addStrikeBlock(new BlockPos(x, y, z));
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
                addStrikeBlock(new BlockPos(x, y, z));
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
        addStrikeBlock(new BlockPos(x2, y2, z2));
        
        LeoEnchantsMod.LOGGER.info("Obsidian Strike: Calculated {} blocks, distance: {} blocks", 
            strikeBlocks.size(), getTotalDistance());
    }
    
    private void addStrikeBlock(BlockPos pos) {
        if (!strikeBlocks.contains(pos)) {
            strikeBlocks.add(pos);
            originalStates.add(null);
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        ticksExisted++;
        
        if (!getWorld().isClient) {
            ServerWorld serverWorld = (ServerWorld) getWorld();
            
            // Strike animation phase
            if (!getStrikeComplete()) {
                for (int i = 0; i < STRIKE_SPEED && currentStrikeIndex < strikeBlocks.size(); i++) {
                    BlockPos pos = strikeBlocks.get(currentStrikeIndex);
                    
                    // Store original state
                    BlockState original = serverWorld.getBlockState(pos);
                    if (currentStrikeIndex < originalStates.size()) {
                        originalStates.set(currentStrikeIndex, original);
                    }
                    
                    // Place obsidian if air or replaceable
                    if (original.isAir() || original.isReplaceable()) {
                        serverWorld.setBlockState(pos, Blocks.OBSIDIAN.getDefaultState());
                        
                        // Spawn strike particles
                        serverWorld.spawnParticles(ParticleTypes.DRAGON_BREATH,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            3, 0.2, 0.2, 0.2, 0.01);
                    }
                    
                    // Check for entities at this position to damage
                    checkAndDamageEntities(serverWorld, pos);
                    
                    currentStrikeIndex++;
                }
                
                // Play strike sound periodically
                if (currentStrikeIndex % 10 == 0) {
                    BlockPos currentPos = strikeBlocks.get(Math.min(currentStrikeIndex, strikeBlocks.size() - 1));
                    serverWorld.playSound(null, currentPos,
                        SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.BLOCKS, 0.5f, 1.5f);
                }
                
                // Check if strike is complete
                if (currentStrikeIndex >= strikeBlocks.size()) {
                    setStrikeComplete(true);
                    
                    // Deal impact damage to target entity
                    dealImpactDamage(serverWorld);
                    
                    serverWorld.playSound(null, endPos.x, endPos.y, endPos.z,
                        SoundEvents.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.BLOCKS, 1.0f, 0.7f);
                }
            }
            
            // Lifetime countdown after strike complete
            if (getStrikeComplete()) {
                setLifetime(getLifetime() + 1);
                
                // Spawn periodic particles
                if (ticksExisted % 20 == 0) {
                    for (int i = 0; i < strikeBlocks.size(); i += 3) {
                        BlockPos pos = strikeBlocks.get(i);
                        serverWorld.spawnParticles(ParticleTypes.SMOKE,
                            pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                            1, 0.2, 0.1, 0.2, 0.01);
                    }
                }
                
                // Despawn after lifetime
                if (getLifetime() >= MAX_LIFETIME_TICKS) {
                    removeStrike(serverWorld);
                    this.discard();
                }
            }
        }
    }
    
    /**
     * Check for and damage entities at a given position
     */
    private void checkAndDamageEntities(ServerWorld serverWorld, BlockPos pos) {
        Box damageBox = new Box(pos).expand(0.5);
        List<Entity> entities = serverWorld.getOtherEntities(this, damageBox);
        
        for (Entity entity : entities) {
            if (entity.getUuid().equals(ownerUUID)) continue;
            if (damagedEntities.contains(entity.getUuid())) continue;
            
            if (entity instanceof LivingEntity livingEntity) {
                float damage = calculateDamage();
                livingEntity.damage(serverWorld, serverWorld.getDamageSources().magic(), damage * 0.5f);
                damagedEntities.add(entity.getUuid());
                
                // Knockback slightly
                Vec3d direction = endPos.subtract(startPos).normalize();
                entity.addVelocity(direction.x * 0.5, 0.2, direction.z * 0.5);
            }
        }
    }
    
    /**
     * Deal impact damage to the target entity when strike completes
     */
    private void dealImpactDamage(ServerWorld serverWorld) {
        float damage = calculateDamage();
        
        // Damage entities near the end point
        Box impactBox = new Box(
            endPos.x - 2, endPos.y - 2, endPos.z - 2,
            endPos.x + 2, endPos.y + 2, endPos.z + 2
        );
        
        List<Entity> entities = serverWorld.getOtherEntities(this, impactBox);
        for (Entity entity : entities) {
            if (entity.getUuid().equals(ownerUUID)) continue;
            
            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.damage(serverWorld, serverWorld.getDamageSources().magic(), damage);
                
                // Strong knockback at impact
                Vec3d knockback = entity.getPos().subtract(endPos).normalize().multiply(1.5);
                entity.addVelocity(knockback.x, 0.5, knockback.z);
                
                LeoEnchantsMod.LOGGER.info("Obsidian Strike dealt {} damage to {}", 
                    damage, entity.getType().getName().getString());
            }
        }
        
        // Impact particles
        serverWorld.spawnParticles(ParticleTypes.EXPLOSION,
            endPos.x, endPos.y, endPos.z, 3, 0.5, 0.5, 0.5, 0);
    }
    
    /**
     * Calculate damage based on distance
     */
    private float calculateDamage() {
        float distance = getTotalDistance();
        float damage = BASE_DAMAGE + (distance * DAMAGE_PER_BLOCK);
        return Math.min(damage, MAX_DAMAGE);
    }
    
    /**
     * Remove all placed obsidian blocks
     */
    private void removeStrike(ServerWorld serverWorld) {
        for (int i = strikeBlocks.size() - 1; i >= 0; i--) {
            BlockPos pos = strikeBlocks.get(i);
            BlockState currentState = serverWorld.getBlockState(pos);
            
            if (currentState.isOf(Blocks.OBSIDIAN)) {
                BlockState original = (i < originalStates.size()) ? originalStates.get(i) : null;
                if (original != null && !original.isOf(Blocks.OBSIDIAN)) {
                    serverWorld.setBlockState(pos, original);
                } else {
                    serverWorld.setBlockState(pos, Blocks.AIR.getDefaultState());
                }
                
                serverWorld.spawnParticles(ParticleTypes.DRAGON_BREATH,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    5, 0.3, 0.3, 0.3, 0.02);
            }
        }
        
        serverWorld.playSound(null, getX(), getY(), getZ(),
            SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.BLOCKS, 1.0f, 0.5f);
    }
    
    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        return false; // Cannot be damaged
    }
    
    // Getters and Setters
    public int getLifetime() {
        return this.dataTracker.get(LIFETIME);
    }
    
    public void setLifetime(int lifetime) {
        this.dataTracker.set(LIFETIME, lifetime);
    }
    
    public boolean getStrikeComplete() {
        return this.dataTracker.get(STRIKE_COMPLETE);
    }
    
    public void setStrikeComplete(boolean complete) {
        this.dataTracker.set(STRIKE_COMPLETE, complete);
    }
    
    public float getTotalDistance() {
        return this.dataTracker.get(TOTAL_DISTANCE);
    }
    
    public void setTotalDistance(float distance) {
        this.dataTracker.set(TOTAL_DISTANCE, distance);
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
    
    public List<BlockPos> getStrikeBlocks() {
        return strikeBlocks;
    }
    
    @Override
    public void readCustomData(ReadView readView) {
        setLifetime(readView.getInt("Lifetime", 0));
        setStrikeComplete(readView.getBoolean("StrikeComplete", false));
        setTotalDistance(readView.getFloat("TotalDistance", 0.0f));
        currentStrikeIndex = readView.getInt("CurrentStrikeIndex", 0);
        ticksExisted = readView.getInt("TicksExisted", 0);
        
        String uuidStr = readView.getString("OwnerUUID", "");
        if (!uuidStr.isEmpty()) {
            try {
                ownerUUID = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException ignored) {}
        }
        
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
        
        if (startPos != null && endPos != null) {
            calculateStrikeBlocks();
        }
    }
    
    @Override
    public void writeCustomData(WriteView writeView) {
        writeView.putInt("Lifetime", getLifetime());
        writeView.putBoolean("StrikeComplete", getStrikeComplete());
        writeView.putFloat("TotalDistance", getTotalDistance());
        writeView.putInt("CurrentStrikeIndex", currentStrikeIndex);
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

