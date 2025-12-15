package com.leo.enchants.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Shadow clones that fly toward the target and strike.
 * Now uses MobEntity for proper player-like rendering.
 * Vulnerable to damage - if destroyed, the owner loses part of their health restoration.
 */
public class ShadowCloneEntity extends MobEntity {

    private static final TrackedData<Float> BODY_YAW = DataTracker.registerData(ShadowCloneEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> HEAD_YAW = DataTracker.registerData(ShadowCloneEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> LIMB_SWING = DataTracker.registerData(ShadowCloneEntity.class, TrackedDataHandlerRegistry.FLOAT);

    private Vec3d startPos = Vec3d.ZERO;
    private Vec3d controlA = Vec3d.ZERO;
    private Vec3d controlB = Vec3d.ZERO;
    private Vec3d targetPos = Vec3d.ZERO;
    private UUID targetEntityUuid = null;
    private int lifetimeTicks = 20;
    private int ageTicks = 0;
    private float damageAmount = 4.0f;
    private UUID ownerUuid;
    private boolean hasAttacked = false;
    private double curveAngleDegrees = 0.0;
    private ItemStack weaponStack = ItemStack.EMPTY;
    private Consumer<ShadowCloneEntity> onDestroyedCallback = null;
    private boolean wasDestroyed = false;

    public ShadowCloneEntity(EntityType<? extends ShadowCloneEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public static DefaultAttributeContainer.Builder createShadowCloneAttributes() {
        return MobEntity.createMobAttributes()
            .add(EntityAttributes.MAX_HEALTH, 10.0)
            .add(EntityAttributes.MOVEMENT_SPEED, 0.0)
            .add(EntityAttributes.ATTACK_DAMAGE, 0.0);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(BODY_YAW, 0.0f);
        builder.add(HEAD_YAW, 0.0f);
        builder.add(LIMB_SWING, 0.0f);
    }

    /**
     * Factory helper that sets up the curve and equipment after constructing the entity.
     */
    public static ShadowCloneEntity create(ServerWorld world, ServerPlayerEntity owner, LivingEntity target, 
                                           ItemStack weapon, double angleDegrees, int travelTicks, float damage,
                                           Consumer<ShadowCloneEntity> onDestroyed) {
        ShadowCloneEntity clone = new ShadowCloneEntity(ModEntities.SHADOW_CLONE, world);
        clone.configure(owner, target, weapon, angleDegrees, travelTicks, damage, onDestroyed);
        return clone;
    }

    private void configure(ServerPlayerEntity owner, LivingEntity target, ItemStack weapon, 
                          double angleDegrees, int travelTicks, float damage, 
                          Consumer<ShadowCloneEntity> onDestroyed) {
        this.ownerUuid = owner.getUuid();
        this.targetEntityUuid = target.getUuid();
        this.lifetimeTicks = Math.max(10, travelTicks);
        this.damageAmount = Math.max(1.0f, damage);
        this.ageTicks = 0;
        this.hasAttacked = false;
        this.noClip = true;
        this.setNoGravity(true);
        this.curveAngleDegrees = angleDegrees;
        this.weaponStack = weapon.copy();
        this.onDestroyedCallback = onDestroyed;
        this.wasDestroyed = false;

        // Start around the player's chest height
        this.startPos = owner.getPos().add(0, owner.getStandingEyeHeight() * 0.5, 0);
        this.targetPos = target.getPos().add(0, target.getHeight() * 0.5, 0);

        setupCurve(angleDegrees);
        setPosition(startPos.x, startPos.y, startPos.z);

        // Orient toward the target initially
        Vec3d dir = targetPos.subtract(startPos);
        float yaw = (float) (MathHelper.atan2(dir.x, dir.z) * (180F / Math.PI));
        this.setYaw(yaw);
        this.setBodyYaw(yaw);
        this.setHeadYaw(yaw);
        
        // Sync to data tracker for client rendering
        this.dataTracker.set(BODY_YAW, yaw);
        this.dataTracker.set(HEAD_YAW, yaw);
    }

    private void setupCurve(double angleDegrees) {
        Vec3d direct = targetPos.subtract(startPos);
        if (direct.lengthSquared() < 0.0001) {
            direct = new Vec3d(0, 0, 1);
        }

        Vec3d forwardFlat = new Vec3d(direct.x, 0, direct.z);
        double horizontal = forwardFlat.length();
        if (horizontal < 0.0001) {
            forwardFlat = new Vec3d(0, 0, 1);
            horizontal = 1.0;
        }
        Vec3d forwardNorm = forwardFlat.normalize();

        double angleRad = Math.toRadians(angleDegrees);
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        Vec3d rotated = new Vec3d(
            forwardNorm.x * cos - forwardNorm.z * sin,
            0,
            forwardNorm.x * sin + forwardNorm.z * cos
        );

        double outwardDistance = Math.max(2.5, horizontal * 0.6);
        Vec3d outward = rotated.multiply(outwardDistance);

        this.controlA = startPos.add(outward).add(0, 0.4, 0);
        this.controlB = targetPos.add(outward.multiply(0.3)).add(0, 0.25, 0);

        // For a straight shot (angle 0) collapse to a line
        if (Math.abs(angleDegrees) < 1e-3) {
            this.controlA = startPos.lerp(targetPos, 0.33);
            this.controlB = startPos.lerp(targetPos, 0.66);
        }
    }

    @Override
    public void tick() {
        super.tick();
        ageTicks++;

        // Update limb swing for animation
        float swing = (float)(ageTicks * 0.5) % 1.0f;
        this.dataTracker.set(LIMB_SWING, swing);

        if (!getWorld().isClient) {
            ServerWorld serverWorld = (ServerWorld) getWorld();
            
            LivingEntity trackedTarget = getTrackedTarget();
            if (trackedTarget == null || !trackedTarget.isAlive()) {
                discard();
                return;
            }

            // Refresh target point so we home and face correctly
            this.targetPos = trackedTarget.getPos().add(0, trackedTarget.getHeight() * 0.5, 0);
            setupCurve(this.curveAngleDegrees);

            double progress = Math.min(1.0, (double) ageTicks / (double) lifetimeTicks);
            Vec3d currentPos = getBezierPosition(progress);

            setPosition(currentPos.x, currentPos.y, currentPos.z);

            // Face the target directly
            Vec3d lookDir = targetPos.subtract(currentPos);
            if (lookDir.lengthSquared() > 1.0e-4) {
                float yaw = (float) (MathHelper.atan2(lookDir.x, lookDir.z) * (180F / Math.PI));
                this.setYaw(yaw);
                this.setBodyYaw(yaw);
                this.setHeadYaw(yaw);
                this.dataTracker.set(BODY_YAW, yaw);
                this.dataTracker.set(HEAD_YAW, yaw);
            }
            
            // Spawn trailing smoke particles while moving
            if (ageTicks % 2 == 0 && !hasAttacked) {
                spawnTrailParticles(serverWorld);
            }

            if (!hasAttacked && ageTicks >= lifetimeTicks) {
                attackTarget(trackedTarget);
                hasAttacked = true;
                // Disappear immediately after attacking
                discard();
            }
        }
    }
    
    /**
     * Spawns shadow trail particles behind the clone as it moves.
     */
    private void spawnTrailParticles(ServerWorld world) {
        double x = getX();
        double y = getY() + 0.5;
        double z = getZ();
        
        // Dark smoke trail
        world.spawnParticles(
            ParticleTypes.SMOKE,
            x + (random.nextDouble() - 0.5) * 0.3,
            y + (random.nextDouble() - 0.5) * 0.8,
            z + (random.nextDouble() - 0.5) * 0.3,
            1, 0, 0, 0, 0.01
        );
        
        // Occasional soul particle
        if (random.nextFloat() < 0.2f) {
            world.spawnParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                x + (random.nextDouble() - 0.5) * 0.2,
                y + random.nextDouble() * 1.0,
                z + (random.nextDouble() - 0.5) * 0.2,
                1, 0, 0.02, 0, 0.01
            );
        }
    }

    private Vec3d getBezierPosition(double t) {
        double u = 1.0 - t;
        double tt = t * t;
        double uu = u * u;
        double uuu = uu * u;
        double ttt = tt * t;

        return startPos.multiply(uuu)
            .add(controlA.multiply(3 * uu * t))
            .add(controlB.multiply(3 * u * tt))
            .add(targetPos.multiply(ttt));
    }

    private LivingEntity getTrackedTarget() {
        if (!(getWorld() instanceof ServerWorld serverWorld)) {
            return null;
        }
        Entity targetEntity = this.targetEntityUuid != null ? serverWorld.getEntity(this.targetEntityUuid) : null;
        if (targetEntity instanceof LivingEntity livingTarget) {
            return livingTarget;
        }
        return null;
    }

    private void attackTarget(LivingEntity livingTarget) {
        if (!(getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        ServerPlayerEntity owner = null;
        if (ownerUuid != null) {
            var maybePlayer = serverWorld.getPlayerByUuid(ownerUuid);
            if (maybePlayer instanceof ServerPlayerEntity serverPlayer) {
                owner = serverPlayer;
            }
        }
        
        DamageSource source = owner != null
            ? serverWorld.getDamageSources().playerAttack(owner)
            : serverWorld.getDamageSources().magic();

        livingTarget.timeUntilRegen = 0;
        livingTarget.damage(serverWorld, source, damageAmount);

        // Small knockback toward the strike direction
        Vec3d knockDir = livingTarget.getPos().subtract(this.getPos()).normalize().multiply(0.35);
        livingTarget.addVelocity(knockDir.x, 0.1, knockDir.z);
        
        // Consume durability from owner's weapon if it's a damageable item
        if (owner != null) {
            consumeWeaponDurability(owner, serverWorld);
        }
        
        // Spawn attack impact particles
        spawnAttackParticles(serverWorld, livingTarget);
    }
    
    /**
     * Consumes durability from the owner's weapon when the shadow clone attacks.
     * Only affects items with durability (tools, weapons, etc.)
     */
    private void consumeWeaponDurability(ServerPlayerEntity owner, ServerWorld world) {
        ItemStack ownerWeapon = owner.getMainHandStack();
        
        // Check if the item has durability (is damageable)
        if (!ownerWeapon.isEmpty() && ownerWeapon.isDamageable()) {
            // Damage the weapon by 1 durability per clone attack
            ownerWeapon.damage(1, owner, EquipmentSlot.MAINHAND);
        }
    }
    
    /**
     * Spawns impact particles when the clone strikes the target.
     */
    private void spawnAttackParticles(ServerWorld world, LivingEntity target) {
        double x = target.getX();
        double y = target.getY() + target.getHeight() / 2;
        double z = target.getZ();
        
        // Dark impact burst
        for (int i = 0; i < 15; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 0.5;
            double offsetY = (random.nextDouble() - 0.5) * 0.8;
            double offsetZ = (random.nextDouble() - 0.5) * 0.5;
            
            world.spawnParticles(
                ParticleTypes.SMOKE,
                x + offsetX, y + offsetY, z + offsetZ,
                1, 0.1, 0.1, 0.1, 0.05
            );
        }
        
        // Damage spark effect
        world.spawnParticles(
            ParticleTypes.CRIT,
            x, y, z,
            8, 0.3, 0.3, 0.3, 0.1
        );
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        // Don't take damage from owner
        if (source.getAttacker() != null && source.getAttacker().getUuid().equals(ownerUuid)) {
            return false;
        }
        
        // Actually take damage now - clones are vulnerable
        boolean result = super.damage(world, source, amount);
        
        // If we died, mark as destroyed and trigger callback
        if (this.isDead() && !wasDestroyed) {
            markDestroyed();
        }
        
        return result;
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        if (!wasDestroyed) {
            markDestroyed();
        }
    }
    
    /**
     * Marks this clone as destroyed, spawns dissipation particles, and triggers callback.
     */
    private void markDestroyed() {
        wasDestroyed = true;
        
        // Spawn dark smoke/shadow dissipation particles
        if (getWorld() instanceof ServerWorld serverWorld) {
            double x = getX();
            double y = getY() + getHeight() / 2;
            double z = getZ();
            
            // Large burst of smoke particles
            for (int i = 0; i < 30; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 0.8;
                double offsetY = (random.nextDouble() - 0.5) * 1.5;
                double offsetZ = (random.nextDouble() - 0.5) * 0.8;
                double velX = (random.nextDouble() - 0.5) * 0.2;
                double velY = random.nextDouble() * 0.15;
                double velZ = (random.nextDouble() - 0.5) * 0.2;
                
                serverWorld.spawnParticles(
                    ParticleTypes.LARGE_SMOKE,
                    x + offsetX, y + offsetY, z + offsetZ,
                    1, velX, velY, velZ, 0.02
                );
            }
            
            // Some soul particles for extra effect
            for (int i = 0; i < 10; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 0.5;
                double offsetY = (random.nextDouble() - 0.5) * 1.2;
                double offsetZ = (random.nextDouble() - 0.5) * 0.5;
                
                serverWorld.spawnParticles(
                    ParticleTypes.SOUL,
                    x + offsetX, y + offsetY, z + offsetZ,
                    1, 0, 0.05, 0, 0.02
                );
            }
        }
        
        if (onDestroyedCallback != null) {
            onDestroyedCallback.accept(this);
        }
    }

    public boolean wasDestroyed() {
        return wasDestroyed;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushAway(Entity entity) {
        // Do nothing - clones phase through entities
    }

    @Override
    public boolean isAffectedBySplashPotions() {
        return false;
    }

    @Override
    public boolean hasNoDrag() {
        return true;
    }

    // Getters for renderer
    public float getSyncedBodyYaw() {
        return this.dataTracker.get(BODY_YAW);
    }

    public float getSyncedHeadYaw() {
        return this.dataTracker.get(HEAD_YAW);
    }

    public float getLimbSwing() {
        return this.dataTracker.get(LIMB_SWING);
    }

    public ItemStack getWeaponStack() {
        return weaponStack;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public int getAgeTicks() {
        return ageTicks;
    }
}
