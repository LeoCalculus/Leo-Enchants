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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class GiantSwordEntity extends Entity {
    
    // Data tracker keys
    private static final TrackedData<Integer> LEVEL = DataTracker.registerData(GiantSwordEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> SWORD_SIZE = DataTracker.registerData(GiantSwordEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> HAS_LANDED = DataTracker.registerData(GiantSwordEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_PHASING = DataTracker.registerData(GiantSwordEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<String> SWORD_TYPE = DataTracker.registerData(GiantSwordEntity.class, TrackedDataHandlerRegistry.STRING);
    
    // Constants
    private static final int LIFETIME_TICKS = 20 * 20; // 20 seconds
    private static final float FALL_SPEED = 2.0f; // Blocks per tick
    
    // Instance variables
    private UUID ownerUUID;
    private int ticksExisted = 0;
    private double targetY; // The Y level to stop at (ground level or bedrock for level 3)
    private boolean hasDealtImpactDamage = false;
    private Set<UUID> entitiesHitDuringPhase = new HashSet<>();
    private int phaseDamageCooldown = 0;
    
    public GiantSwordEntity(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true; // Sword doesn't collide with blocks
        this.setNoGravity(true);
    }
    
    public GiantSwordEntity(World world, double x, double y, double z, int level, UUID ownerUUID, String swordType) {
        this(ModEntities.GIANT_SWORD, world);
        this.setPosition(x, y, z);
        this.ownerUUID = ownerUUID;
        this.setLevel(level);
        this.setSwordSize(getSizeForLevel(level));
        this.setHasLanded(false);
        this.setIsPhasing(false);
        this.setSwordType(swordType);
        this.targetY = findTargetY(x, z);
    }
    
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(LEVEL, 1);
        builder.add(SWORD_SIZE, 50.0f);
        builder.add(HAS_LANDED, false);
        builder.add(IS_PHASING, false);
        builder.add(SWORD_TYPE, "minecraft:diamond_sword"); // Default to diamond
    }
    
    private float getSizeForLevel(int level) {
        return switch (level) {
            case 1 -> 50.0f;
            case 2 -> 80.0f;
            case 3 -> 100.0f;
            default -> 50.0f;
        };
    }
    
    private float getRadiusForLevel(int level) {
        return switch (level) {
            case 1 -> 25.0f;
            case 2 -> 50.0f;
            case 3 -> 80.0f;
            default -> 25.0f;
        };
    }
    
    private float getDamageForLevel(int level) {
        return switch (level) {
            case 1 -> 20.0f;
            case 2 -> 50.0f;
            case 3 -> 100.0f;
            default -> 20.0f;
        };
    }
    
    private double findTargetY(double x, double z) {
        int level = getLevel();
        BlockPos.Mutable pos = new BlockPos.Mutable((int) x, (int) getY(), (int) z);
        
        // For level 3, target is bedrock level (y = -64 in overworld, but we check for bedrock)
        if (level >= 3) {
            // Find the lowest non-bedrock Y
            for (int y = getWorld().getBottomY(); y < getWorld().getTopYInclusive(); y++) {
                pos.setY(y);
                BlockState state = getWorld().getBlockState(pos);
                if (state.isOf(Blocks.BEDROCK)) {
                    return y + 1; // Stop just above bedrock
                }
            }
            return getWorld().getBottomY();
        } else {
            // For level 1-2, find the ground level
            for (int y = (int) getY(); y > getWorld().getBottomY(); y--) {
                pos.setY(y);
                BlockState state = getWorld().getBlockState(pos);
                if (!state.isAir() && !state.isLiquid()) {
                    return y + 1; // Stop at ground level
                }
            }
            return getWorld().getBottomY();
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        ticksExisted++;
        
        // Despawn after lifetime
        if (ticksExisted >= LIFETIME_TICKS) {
            this.discard();
            return;
        }
        
        if (!getWorld().isClient) {
            ServerWorld serverWorld = (ServerWorld) getWorld();
            
            if (!getHasLanded()) {
                // Falling phase
                double newY = getY() - FALL_SPEED;
                
                // For level 3, destroy blocks in path (except bedrock)
                if (getLevel() >= 3) {
                    destroyBlocksInPath(serverWorld, newY);
                }
                
                // Deal damage to surrounding entities while falling
                dealFallingDamage(serverWorld);
                
                // Check if we've reached the target
                if (newY <= targetY) {
                    newY = targetY;
                    onLand(serverWorld);
                }
                
                setPosition(getX(), newY, getZ());
                
                // Spawn falling particles
                spawnFallingParticles(serverWorld);
            }
            // No continuous damage after landing - sword just stays in ground
        }
        
    }
    
    private void onLand(ServerWorld serverWorld) {
        setHasLanded(true);
        
        if (!hasDealtImpactDamage) {
            hasDealtImpactDamage = true;
            dealImpactDamage(serverWorld);
            
            // Play impact sound
            serverWorld.playSound(null, getX(), getY(), getZ(), 
                SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 2.0f, 0.5f);
            
            // Spawn impact particles
            spawnImpactParticles(serverWorld);
            
            // No continuous damage after landing - sword stays in ground until despawn
        }
    }
    
    private void dealImpactDamage(ServerWorld serverWorld) {
        int level = getLevel();
        float radius = getRadiusForLevel(level);
        float damage = getDamageForLevel(level);
        
        Box damageBox = new Box(
            getX() - radius, getY() - radius, getZ() - radius,
            getX() + radius, getY() + radius, getZ() + radius
        );
        
        List<Entity> entities = serverWorld.getOtherEntities(this, damageBox);
        
        for (Entity entity : entities) {
            // Skip the owner
            if (entity.getUuid().equals(ownerUUID)) {
                continue;
            }
            
            if (entity instanceof LivingEntity livingEntity) {
                double distance = entity.getPos().distanceTo(getPos());
                if (distance <= radius) {
                    // Damage falls off with distance slightly
                    float actualDamage = (float) (damage * (1.0 - (distance / radius) * 0.3));
                    livingEntity.damage(serverWorld, serverWorld.getDamageSources().magic(), actualDamage);
                    
                    // Knockback
                    Vec3d knockback = entity.getPos().subtract(getPos()).normalize().multiply(2.0);
                    entity.addVelocity(knockback.x, 0.5, knockback.z);
                }
            }
        }
        
        LeoEnchantsMod.LOGGER.info("Giant Sword Level {} dealt impact damage in {} block radius", level, radius);
    }
    
    private void dealFallingDamage(ServerWorld serverWorld) {
        int level = getLevel();
        float radius = getRadiusForLevel(level);
        float damage = getDamageForLevel(level);
        
        // Damage box around the sword's current position
        Box damageBox = new Box(
            getX() - radius, getY() - 5, getZ() - radius,
            getX() + radius, getY() + 5, getZ() + radius
        );
        
        List<Entity> entities = serverWorld.getOtherEntities(this, damageBox);
        
        for (Entity entity : entities) {
            // Skip the owner
            if (entity.getUuid().equals(ownerUUID)) {
                continue;
            }
            
            if (entity instanceof LivingEntity livingEntity) {
                double distance = Math.sqrt(
                    Math.pow(entity.getX() - getX(), 2) + 
                    Math.pow(entity.getZ() - getZ(), 2)
                );
                
                if (distance <= radius) {
                    // Check if we haven't damaged this entity recently
                    if (!entitiesHitDuringPhase.contains(entity.getUuid())) {
                        livingEntity.damage(serverWorld, serverWorld.getDamageSources().magic(), damage);
                        entitiesHitDuringPhase.add(entity.getUuid());
                        
                        // Knockback away from sword
                        Vec3d knockback = entity.getPos().subtract(getPos()).normalize().multiply(1.5);
                        entity.addVelocity(knockback.x, 0.3, knockback.z);
                    }
                }
            }
        }
    }
    
    private void spawnFallingParticles(ServerWorld serverWorld) {
        // Spawn particles around the sword as it falls
        float size = getSwordSize();
        for (int i = 0; i < 5; i++) {
            double offsetX = (random.nextDouble() - 0.5) * size * 0.1;
            double offsetZ = (random.nextDouble() - 0.5) * size * 0.1;
            serverWorld.spawnParticles(ParticleTypes.CLOUD, 
                getX() + offsetX, getY(), getZ() + offsetZ, 
                1, 0, 0, 0, 0.05);
        }
    }
    
    private void spawnImpactParticles(ServerWorld serverWorld) {
        float radius = getRadiusForLevel(getLevel());
        int particleCount = (int) (radius * 10);
        
        // Explosion ring particles
        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI / particleCount) * i;
            double x = getX() + Math.cos(angle) * radius * 0.5;
            double z = getZ() + Math.sin(angle) * radius * 0.5;
            serverWorld.spawnParticles(ParticleTypes.EXPLOSION, x, getY(), z, 1, 0, 0, 0, 0);
        }
        
        // Central explosion
        serverWorld.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
    }
    
    private void destroyBlocksInPath(ServerWorld serverWorld, double targetY) {
        // Destroy blocks in a radius around the sword's path
        int destroyRadius = 3; // Destroy blocks in a 3 block radius
        int centerX = (int) Math.floor(getX());
        int centerZ = (int) Math.floor(getZ());
        
        // Destroy from current Y to target Y
        int startY = (int) Math.floor(targetY);
        int endY = (int) Math.floor(getY());
        
        for (int y = startY; y <= endY; y++) {
            for (int x = centerX - destroyRadius; x <= centerX + destroyRadius; x++) {
                for (int z = centerZ - destroyRadius; z <= centerZ + destroyRadius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = serverWorld.getBlockState(pos);
                    
                    // Skip bedrock and air
                    if (state.isOf(Blocks.BEDROCK) || state.isAir()) {
                        continue;
                    }
                    
                    // Calculate distance from center
                    double dist = Math.sqrt(Math.pow(x - getX(), 2) + Math.pow(z - getZ(), 2));
                    if (dist <= destroyRadius) {
                        // Destroy the block without drops
                        serverWorld.breakBlock(pos, false);
                    }
                }
            }
        }
    }
    
    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        // Giant sword entity cannot be damaged
        return false;
    }
    
    // Getters and Setters for tracked data
    public int getLevel() {
        return this.dataTracker.get(LEVEL);
    }
    
    public void setLevel(int level) {
        this.dataTracker.set(LEVEL, level);
    }
    
    public float getSwordSize() {
        return this.dataTracker.get(SWORD_SIZE);
    }
    
    public void setSwordSize(float size) {
        this.dataTracker.set(SWORD_SIZE, size);
    }
    
    public boolean getHasLanded() {
        return this.dataTracker.get(HAS_LANDED);
    }
    
    public void setHasLanded(boolean landed) {
        this.dataTracker.set(HAS_LANDED, landed);
    }
    
    public boolean getIsPhasing() {
        return this.dataTracker.get(IS_PHASING);
    }
    
    public void setIsPhasing(boolean phasing) {
        this.dataTracker.set(IS_PHASING, phasing);
    }
    
    public UUID getOwnerUUID() {
        return ownerUUID;
    }
    
    public int getTicksExisted() {
        return ticksExisted;
    }
    
    public String getSwordType() {
        return this.dataTracker.get(SWORD_TYPE);
    }
    
    public void setSwordType(String swordType) {
        this.dataTracker.set(SWORD_TYPE, swordType);
    }
    
    @Override
    public void readCustomData(ReadView readView) {
        setLevel(readView.getInt("Level", 1));
        setSwordSize(readView.getFloat("SwordSize", 50.0f));
        setHasLanded(readView.getBoolean("HasLanded", false));
        setIsPhasing(readView.getBoolean("IsPhasing", false));
        setSwordType(readView.getString("SwordType", "minecraft:diamond_sword"));
        
        String uuidStr = readView.getString("OwnerUUID", "");
        if (!uuidStr.isEmpty()) {
            try {
                ownerUUID = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException ignored) {}
        }
        
        ticksExisted = readView.getInt("TicksExisted", 0);
        targetY = readView.getDouble("TargetY", 0.0);
        hasDealtImpactDamage = readView.getBoolean("HasDealtImpactDamage", false);
    }
    
    @Override
    public void writeCustomData(WriteView writeView) {
        writeView.putInt("Level", getLevel());
        writeView.putFloat("SwordSize", getSwordSize());
        writeView.putBoolean("HasLanded", getHasLanded());
        writeView.putBoolean("IsPhasing", getIsPhasing());
        writeView.putString("SwordType", getSwordType());
        if (ownerUUID != null) {
            writeView.putString("OwnerUUID", ownerUUID.toString());
        }
        writeView.putInt("TicksExisted", ticksExisted);
        writeView.putDouble("TargetY", targetY);
        writeView.putBoolean("HasDealtImpactDamage", hasDealtImpactDamage);
    }
}
