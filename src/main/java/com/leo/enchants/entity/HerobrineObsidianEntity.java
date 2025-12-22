package com.leo.enchants.entity;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

/**
 * Floating obsidian projectile summoned by Herobrine.
 * Hovers in air, then launches toward the target.
 * Damage scales with distance - further = more damage (like Obsidian Lore).
 */
public class HerobrineObsidianEntity extends Entity {
    
    private static final TrackedData<Integer> DELAY = DataTracker.registerData(HerobrineObsidianEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> STATE = DataTracker.registerData(HerobrineObsidianEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> START_DISTANCE = DataTracker.registerData(HerobrineObsidianEntity.class, TrackedDataHandlerRegistry.FLOAT);
    
    // States
    private static final int STATE_HOVERING = 0;
    private static final int STATE_CHARGING = 1;
    private static final int STATE_FLYING = 2;
    
    // Constants
    private static final int HOVER_TIME = 20; // 1 second hover
    private static final int CHARGE_TIME = 10; // 0.5 second charge
    private static final float FLY_SPEED = 1.5f;
    private static final float BASE_DAMAGE = 5.0f;
    private static final float DAMAGE_PER_BLOCK = 0.8f;
    private static final float MAX_DAMAGE = 40.0f;
    private static final int MAX_LIFETIME = 200; // 10 seconds max
    
    private UUID ownerUUID;
    private UUID targetUUID;
    private Vec3d startPos;
    private int ticksExisted = 0;
    private int stateTimer = 0;
    private boolean hasHit = false;
    
    public HerobrineObsidianEntity(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }
    
    public HerobrineObsidianEntity(World world, Entity owner, LivingEntity target, Vec3d spawnPos) {
        this(ModEntities.HEROBRINE_OBSIDIAN, world);
        this.ownerUUID = owner.getUuid();
        this.targetUUID = target.getUuid();
        this.startPos = spawnPos;
        this.setPosition(spawnPos.x, spawnPos.y, spawnPos.z);
        this.setStartDistance((float) spawnPos.distanceTo(target.getPos()));
    }
    
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(DELAY, 0);
        builder.add(STATE, STATE_HOVERING);
        builder.add(START_DISTANCE, 0.0f);
    }
    
    @Override
    public void tick() {
        super.tick();
        ticksExisted++;
        
        // Check lifetime
        if (ticksExisted > MAX_LIFETIME) {
            this.discard();
            return;
        }
        
        // Wait for delay
        if (getDelay() > 0) {
            setDelay(getDelay() - 1);
            return;
        }
        
        if (!getWorld().isClient) {
            ServerWorld serverWorld = (ServerWorld) getWorld();
            
            int state = getState();
            stateTimer++;
            
            switch (state) {
                case STATE_HOVERING -> tickHovering(serverWorld);
                case STATE_CHARGING -> tickCharging(serverWorld);
                case STATE_FLYING -> tickFlying(serverWorld);
            }
        }
    }
    
    private void tickHovering(ServerWorld world) {
        // Float menacingly with slight bobbing
        double bobOffset = Math.sin(ticksExisted * 0.2) * 0.05;
        setVelocity(0, bobOffset, 0);
        
        // Spawn hovering particles
        if (ticksExisted % 5 == 0) {
            world.spawnParticles(
                ParticleTypes.DRAGON_BREATH,
                getX(), getY(), getZ(),
                2, 0.2, 0.2, 0.2, 0.01
            );
        }
        
        if (stateTimer >= HOVER_TIME) {
            setState(STATE_CHARGING);
            stateTimer = 0;
            world.playSound(null, getBlockPos(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.HOSTILE, 0.8f, 1.5f);
        }
    }
    
    private void tickCharging(ServerWorld world) {
        // Shake/vibrate effect
        double shakeAmount = 0.1;
        setPosition(
            getX() + (random.nextDouble() - 0.5) * shakeAmount,
            getY() + (random.nextDouble() - 0.5) * shakeAmount,
            getZ() + (random.nextDouble() - 0.5) * shakeAmount
        );
        
        // Intense particles
        world.spawnParticles(
            ParticleTypes.SMOKE,
            getX(), getY(), getZ(),
            3, 0.1, 0.1, 0.1, 0.02
        );
        
        if (stateTimer >= CHARGE_TIME) {
            setState(STATE_FLYING);
            stateTimer = 0;
            startPos = getPos(); // Update start position for damage calculation
            world.playSound(null, getBlockPos(), SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 0.5f, 1.5f);
        }
    }
    
    private void tickFlying(ServerWorld world) {
        if (hasHit) {
            discard();
            return;
        }
        
        // Find target
        Entity target = world.getEntity(targetUUID);
        if (target == null || !target.isAlive()) {
            discard();
            return;
        }
        
        // Skip creative mode players
        if (target instanceof PlayerEntity player && player.isCreative()) {
            discard();
            return;
        }
        
        // Calculate direction to target
        Vec3d targetPos = target.getPos().add(0, target.getHeight() / 2, 0);
        Vec3d currentPos = getPos();
        Vec3d direction = targetPos.subtract(currentPos).normalize();
        
        // Move toward target (directly update position for reliable movement)
        Vec3d newPos = currentPos.add(direction.multiply(FLY_SPEED));
        setPosition(newPos.x, newPos.y, newPos.z);
        
        // Spawn trail particles
        world.spawnParticles(
            new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.OBSIDIAN.getDefaultState()),
            getX(), getY(), getZ(),
            5, 0.1, 0.1, 0.1, 0.05
        );
        
        world.spawnParticles(
            ParticleTypes.DRAGON_BREATH,
            getX(), getY(), getZ(),
            2, 0.1, 0.1, 0.1, 0.02
        );
        
        // Check for hit
        double hitDistance = 2.0;
        if (getPos().distanceTo(targetPos) < hitDistance) {
            onHitTarget(world, target);
        }
        
        // Check for any entity collision
        Box hitBox = getBoundingBox().expand(0.5);
        List<Entity> entities = world.getOtherEntities(this, hitBox);
        for (Entity entity : entities) {
            if (entity.getUuid().equals(ownerUUID)) continue;
            if (entity instanceof PlayerEntity player && !player.isCreative()) {
                onHitTarget(world, player);
                break;
            }
        }
    }
    
    private void onHitTarget(ServerWorld world, Entity target) {
        if (hasHit) return;
        hasHit = true;
        
        // Calculate damage based on distance traveled
        float distance = startPos != null ? (float) startPos.distanceTo(target.getPos()) : getStartDistance();
        float damage = BASE_DAMAGE + (distance * DAMAGE_PER_BLOCK);
        damage = Math.min(damage, MAX_DAMAGE);
        
        // Deal damage
        if (target instanceof LivingEntity livingTarget) {
            livingTarget.damage(world, world.getDamageSources().magic(), damage);
            
            // Knockback
            Vec3d knockback = target.getPos().subtract(getPos()).normalize().multiply(0.5);
            target.addVelocity(knockback.x, 0.3, knockback.z);
        }
        
        // Impact effects
        world.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.HOSTILE, 0.7f, 0.8f);
        
        // Explosion of obsidian particles
        for (int i = 0; i < 20; i++) {
            world.spawnParticles(
                new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.OBSIDIAN.getDefaultState()),
                target.getX(), target.getY() + target.getHeight() / 2, target.getZ(),
                1, 0.3, 0.3, 0.3, 0.2
            );
        }
        
        world.spawnParticles(
            ParticleTypes.EXPLOSION,
            target.getX(), target.getY() + target.getHeight() / 2, target.getZ(),
            1, 0, 0, 0, 0
        );
        
        discard();
    }
    
    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        return false; // Cannot be damaged
    }
    
    // Getters and Setters
    public int getDelay() {
        return this.dataTracker.get(DELAY);
    }
    
    public void setDelay(int delay) {
        this.dataTracker.set(DELAY, delay);
    }
    
    public int getState() {
        return this.dataTracker.get(STATE);
    }
    
    public void setState(int state) {
        this.dataTracker.set(STATE, state);
    }
    
    public float getStartDistance() {
        return this.dataTracker.get(START_DISTANCE);
    }
    
    public void setStartDistance(float distance) {
        this.dataTracker.set(START_DISTANCE, distance);
    }
    
    @Override
    public void readCustomData(ReadView readView) {
        setDelay(readView.getInt("Delay", 0));
        setState(readView.getInt("State", STATE_HOVERING));
        setStartDistance(readView.getFloat("StartDistance", 0.0f));
        ticksExisted = readView.getInt("TicksExisted", 0);
        stateTimer = readView.getInt("StateTimer", 0);
        hasHit = readView.getBoolean("HasHit", false);
        
        String ownerStr = readView.getString("OwnerUUID", "");
        if (!ownerStr.isEmpty()) {
            try {
                ownerUUID = UUID.fromString(ownerStr);
            } catch (IllegalArgumentException ignored) {}
        }
        
        String targetStr = readView.getString("TargetUUID", "");
        if (!targetStr.isEmpty()) {
            try {
                targetUUID = UUID.fromString(targetStr);
            } catch (IllegalArgumentException ignored) {}
        }
        
        if (readView.contains("StartPosX")) {
            startPos = new Vec3d(
                readView.getDouble("StartPosX", 0),
                readView.getDouble("StartPosY", 0),
                readView.getDouble("StartPosZ", 0)
            );
        }
    }
    
    @Override
    public void writeCustomData(WriteView writeView) {
        writeView.putInt("Delay", getDelay());
        writeView.putInt("State", getState());
        writeView.putFloat("StartDistance", getStartDistance());
        writeView.putInt("TicksExisted", ticksExisted);
        writeView.putInt("StateTimer", stateTimer);
        writeView.putBoolean("HasHit", hasHit);
        
        if (ownerUUID != null) {
            writeView.putString("OwnerUUID", ownerUUID.toString());
        }
        if (targetUUID != null) {
            writeView.putString("TargetUUID", targetUUID.toString());
        }
        if (startPos != null) {
            writeView.putDouble("StartPosX", startPos.x);
            writeView.putDouble("StartPosY", startPos.y);
            writeView.putDouble("StartPosZ", startPos.z);
        }
    }
}

