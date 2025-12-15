package com.leo.enchants.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.world.World;
import net.minecraft.util.math.random.Random;

/**
 * Entity that displays a floating "1" or "0" digit that rises and fades away.
 * Used for the de-enchant item's disintegration visual effect.
 */
public class DigitDisintegrationEntity extends Entity {
    
    // Tracked data for client-side rendering
    private static final TrackedData<Boolean> IS_ONE = DataTracker.registerData(DigitDisintegrationEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> AGE_TICKS = DataTracker.registerData(DigitDisintegrationEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> SCALE = DataTracker.registerData(DigitDisintegrationEntity.class, TrackedDataHandlerRegistry.FLOAT);
    
    private static final int MAX_LIFETIME = 40; // 2 seconds at 20 ticks/second
    
    private double velocityX;
    private double velocityY;
    private double velocityZ;
    private float rotationSpeed;
    
    public DigitDisintegrationEntity(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
        this.velocityX = 0;
        this.velocityY = 0.05; // Default upward drift
        this.velocityZ = 0;
        this.rotationSpeed = 0;
    }
    
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(IS_ONE, true);
        builder.add(AGE_TICKS, 0);
        builder.add(SCALE, 0.3f);
    }
    
    /**
     * Sets whether this digit displays "1" (true) or "0" (false).
     */
    public void setRandomDigit(boolean isOne) {
        this.dataTracker.set(IS_ONE, isOne);
    }
    
    /**
     * Sets random velocity for natural-looking dispersion.
     */
    public void setRandomVelocity(Random random) {
        this.velocityX = (random.nextDouble() - 0.5) * 0.15;
        this.velocityY = 0.03 + random.nextDouble() * 0.08;
        this.velocityZ = (random.nextDouble() - 0.5) * 0.15;
        this.rotationSpeed = (random.nextFloat() - 0.5f) * 20.0f;
        this.dataTracker.set(SCALE, 0.2f + random.nextFloat() * 0.3f);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        int age = this.dataTracker.get(AGE_TICKS);
        age++;
        this.dataTracker.set(AGE_TICKS, age);
        
        // Move with velocity
        setPosition(getX() + velocityX, getY() + velocityY, getZ() + velocityZ);
        
        // Slow down horizontal movement over time
        velocityX *= 0.95;
        velocityZ *= 0.95;
        
        // Update rotation
        this.setYaw(this.getYaw() + rotationSpeed);
        
        // Discard after lifetime expires
        if (age >= MAX_LIFETIME) {
            this.discard();
        }
    }
    
    /**
     * Gets whether this digit is a "1" (true) or "0" (false).
     */
    public boolean isOne() {
        return this.dataTracker.get(IS_ONE);
    }
    
    /**
     * Gets the current age in ticks.
     */
    public int getAgeTicks() {
        return this.dataTracker.get(AGE_TICKS);
    }
    
    /**
     * Gets the maximum lifetime in ticks.
     */
    public int getMaxLifetime() {
        return MAX_LIFETIME;
    }
    
    /**
     * Gets the alpha value based on age (fades out over time).
     */
    public float getAlpha() {
        int age = getAgeTicks();
        // Start fading at 50% of lifetime
        if (age < MAX_LIFETIME / 2) {
            return 1.0f;
        }
        return 1.0f - ((float)(age - MAX_LIFETIME / 2) / (float)(MAX_LIFETIME / 2));
    }
    
    /**
     * Gets the scale factor for rendering.
     */
    public float getScale() {
        return this.dataTracker.get(SCALE);
    }
    
    @Override
    public void readCustomData(ReadView readView) {
        this.dataTracker.set(IS_ONE, readView.getBoolean("IsOne", true));
        this.dataTracker.set(AGE_TICKS, readView.getInt("AgeTicks", 0));
        this.dataTracker.set(SCALE, readView.getFloat("Scale", 0.3f));
    }
    
    @Override
    public void writeCustomData(WriteView writeView) {
        writeView.putBoolean("IsOne", this.dataTracker.get(IS_ONE));
        writeView.putInt("AgeTicks", this.dataTracker.get(AGE_TICKS));
        writeView.putFloat("Scale", this.dataTracker.get(SCALE));
    }
    
    @Override
    public boolean shouldRender(double distance) {
        return distance < 256 * 256; // Render within 256 blocks
    }
    
    @Override
    public boolean isAttackable() {
        return false;
    }
    
    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        // Digit entities cannot be damaged
        return false;
    }
}
