package com.leo.enchants.monster;

import com.leo.enchants.mixin.MissileCreeperAccessor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

/**
 * AI Goal that makes a creeper act as a missile.
 * The creeper charges up, launches toward the player, and explodes on arrival.
 */
public class CreeperMissileGoal extends Goal {
    
    private final CreeperEntity creeper;
    
    // Missile states
    private enum State { IDLE, CHARGING, FLYING }
    private State state = State.IDLE;
    
    private int chargeTimer = 0;
    private int flightTimer = 0;
    
    // Timings
    private static final int CHARGE_TIME = 40; // 2 seconds before launch
    private static final int MAX_FLIGHT_TIME = 100; // 5 seconds max flight
    private static final double FLIGHT_SPEED = 0.9;
    private static final double ARRIVAL_DISTANCE = 2.5;
    
    // Target tracking
    private LivingEntity missileTarget = null;
    
    // Track if this creeper has already rolled for missile mode
    private boolean hasRolled = false;
    private boolean isMissileCreeper = false;
    
    public CreeperMissileGoal(CreeperEntity creeper) {
        this.creeper = creeper;
        // Take control of ALL behaviors to prevent normal creeper AI
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP, Control.TARGET));
    }
    
    @Override
    public boolean canStart() {
        // If already in missile mode, keep running
        if (state != State.IDLE) {
            return true;
        }
        
        LivingEntity target = creeper.getTarget();
        if (target == null || !target.isAlive()) {
            hasRolled = false; // Reset roll when no target
            return false;
        }
        
        // Check distance - only activate missile mode at medium range (4-20 blocks)
        double distanceSq = creeper.squaredDistanceTo(target);
        if (distanceSq < 16.0 || distanceSq > 400.0) {
            return false;
        }
        
        // Roll only ONCE per target acquisition
        if (!hasRolled) {
            hasRolled = true;
            isMissileCreeper = creeper.getWorld().random.nextBoolean(); // 50% chance
        }
        
        if (isMissileCreeper) {
            missileTarget = target;
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean shouldContinue() {
        // Always continue if flying or charging - we're committed
        if (state == State.FLYING) {
            if (flightTimer > MAX_FLIGHT_TIME) {
                explode();
                return false;
            }
            return true;
        }
        
        if (state == State.CHARGING) {
            return true;
        }
        
        // IDLE state
        if (missileTarget == null || !missileTarget.isAlive()) {
            return false;
        }
        
        return isMissileCreeper;
    }
    
    @Override
    public void start() {
        if (state == State.IDLE && missileTarget != null) {
            // Begin charging phase
            state = State.CHARGING;
            chargeTimer = 0;
            flightTimer = 0;
            
            // DON'T call ignite() - that triggers normal explosion!
            // Just play warning sound
            creeper.getWorld().playSound(null, creeper.getX(), creeper.getY(), creeper.getZ(),
                SoundEvents.ENTITY_CREEPER_PRIMED, SoundCategory.HOSTILE, 1.5F, 0.5F);
        }
    }
    
    @Override
    public void tick() {
        // Keep the creeper from exploding normally by resetting fuse
        creeper.setFuseSpeed(-1);
        
        if (state == State.CHARGING) {
            tickCharging();
        } else if (state == State.FLYING) {
            tickFlying();
        }
    }
    
    private void tickCharging() {
        chargeTimer++;
        
        // Stop all movement during charge
        creeper.getNavigation().stop();
        creeper.setVelocity(0, Math.min(0, creeper.getVelocity().y), 0); // Only allow falling
        creeper.velocityModified = true;
        
        // Rotate body and head toward target
        if (missileTarget != null && missileTarget.isAlive()) {
            rotateTowardTarget(missileTarget);
        }
        
        // Spawn charging particles - getting more intense
        if (creeper.getWorld() instanceof ServerWorld serverWorld) {
            int particleCount = 3 + (chargeTimer / 10);
            serverWorld.spawnParticles(ParticleTypes.FLAME,
                creeper.getX(), creeper.getY() + 0.8, creeper.getZ(),
                particleCount, 0.3, 0.4, 0.3, 0.02);
            
            // Smoke particles
            serverWorld.spawnParticles(ParticleTypes.SMOKE,
                creeper.getX(), creeper.getY() + 1.2, creeper.getZ(),
                2, 0.2, 0.3, 0.2, 0.01);
            
            // Soul fire particles near the end of charge
            if (chargeTimer > CHARGE_TIME - 10) {
                serverWorld.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    creeper.getX(), creeper.getY() + 0.5, creeper.getZ(),
                    3, 0.2, 0.2, 0.2, 0.05);
            }
        }
        
        // Play tick sound periodically
        if (chargeTimer % 10 == 0) {
            creeper.getWorld().playSound(null, creeper.getX(), creeper.getY(), creeper.getZ(),
                SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), SoundCategory.HOSTILE, 0.5F, 
                1.0F + (chargeTimer / (float) CHARGE_TIME));
        }
        
        // Launch when charged
        if (chargeTimer >= CHARGE_TIME) {
            launch();
        }
    }
    
    private void launch() {
        state = State.FLYING;
        flightTimer = 0;
        
        // Disable gravity for flight
        creeper.setNoGravity(true);
        
        // Play launch sound
        creeper.getWorld().playSound(null, creeper.getX(), creeper.getY(), creeper.getZ(),
            SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.HOSTILE, 2.0F, 0.6F);
        
        // Initial upward boost
        Vec3d initialVelocity = new Vec3d(0, 0.6, 0);
        if (missileTarget != null && missileTarget.isAlive()) {
            Vec3d toTarget = missileTarget.getPos().subtract(creeper.getPos()).normalize();
            initialVelocity = toTarget.multiply(0.5).add(0, 0.5, 0);
        }
        creeper.setVelocity(initialVelocity);
        creeper.velocityModified = true;
    }
    
    private void tickFlying() {
        flightTimer++;
        
        // Keep resetting fuse
        creeper.setFuseSpeed(-1);
        
        // Calculate direction to target
        Vec3d targetPos;
        if (missileTarget != null && missileTarget.isAlive()) {
            targetPos = missileTarget.getPos().add(0, missileTarget.getHeight() / 2, 0);
        } else {
            // Target died/gone - explode immediately
            explode();
            return;
        }
        
        Vec3d creeperPos = creeper.getPos().add(0, creeper.getHeight() / 2, 0);
        Vec3d direction = targetPos.subtract(creeperPos).normalize();
        
        // Apply velocity toward target
        Vec3d velocity = direction.multiply(FLIGHT_SPEED);
        creeper.setVelocity(velocity);
        creeper.velocityModified = true;
        
        // Rotate body and head toward target
        rotateTowardTarget(missileTarget);
        
        // Spawn trail particles
        if (creeper.getWorld() instanceof ServerWorld serverWorld) {
            // Fire trail
            serverWorld.spawnParticles(ParticleTypes.FLAME,
                creeper.getX(), creeper.getY() + 0.5, creeper.getZ(),
                10, 0.2, 0.3, 0.2, 0.08);
            
            // Smoke trail
            serverWorld.spawnParticles(ParticleTypes.LARGE_SMOKE,
                creeper.getX(), creeper.getY(), creeper.getZ(),
                5, 0.3, 0.3, 0.3, 0.02);
        }
        
        // Check arrival
        double distanceToTarget = creeper.distanceTo(missileTarget);
        if (distanceToTarget <= ARRIVAL_DISTANCE) {
            explode();
            return;
        }
        
        // Timeout explosion
        if (flightTimer > MAX_FLIGHT_TIME) {
            explode();
            return;
        }
        
        // Wall collision (after brief launch period)
        if (flightTimer > 8 && (creeper.horizontalCollision || creeper.verticalCollision)) {
            explode();
        }
    }
    
    /**
     * Rotate the creeper's body and head to face the target.
     * Also sets missile mode data for client-side body tilt rendering.
     */
    private void rotateTowardTarget(LivingEntity target) {
        double dx = target.getX() - creeper.getX();
        double dy = (target.getY() + target.getHeight() / 2) - (creeper.getY() + creeper.getHeight() / 2);
        double dz = target.getZ() - creeper.getZ();
        
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        
        // Calculate yaw (horizontal rotation) - Minecraft uses -Z as forward
        float targetYaw = (float) (MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        
        // Calculate pitch (vertical rotation)
        float targetPitch = (float) -(MathHelper.atan2(dy, horizontalDist) * (180.0 / Math.PI));
        
        // Wrap yaw to valid range
        targetYaw = MathHelper.wrapDegrees(targetYaw);
        
        // Apply rotation to body and head
        creeper.setYaw(targetYaw);
        creeper.setBodyYaw(targetYaw);
        creeper.setHeadYaw(targetYaw);
        creeper.setPitch(MathHelper.clamp(targetPitch, -90.0F, 90.0F));
        
        // Set missile mode data for client-side rendering (body tilt)
        if (creeper instanceof MissileCreeperAccessor accessor) {
            accessor.leo_enchants$setMissileMode(state == State.CHARGING || state == State.FLYING);
            accessor.leo_enchants$setMissilePitch(targetPitch);
        }
    }
    
    private void explode() {
        // Reset gravity first
        creeper.setNoGravity(false);
        
        // Create explosion
        if (!creeper.getWorld().isClient()) {
            float explosionPower = creeper.isCharged() ? 6.0F : 3.0F;
            
            creeper.getWorld().createExplosion(
                creeper,
                creeper.getX(),
                creeper.getY() + 0.5,
                creeper.getZ(),
                explosionPower,
                false,
                net.minecraft.world.World.ExplosionSourceType.MOB
            );
            
            // Remove the creeper
            creeper.discard();
        }
        
        state = State.IDLE;
    }
    
    @Override
    public void stop() {
        // Reset gravity if flying
        creeper.setNoGravity(false);
        
        // Reset missile mode for rendering
        if (creeper instanceof MissileCreeperAccessor accessor) {
            accessor.leo_enchants$setMissileMode(false);
            accessor.leo_enchants$setMissilePitch(0.0F);
        }
        
        state = State.IDLE;
        chargeTimer = 0;
        flightTimer = 0;
        missileTarget = null;
        // Keep hasRolled and isMissileCreeper to remember the roll
    }
}
