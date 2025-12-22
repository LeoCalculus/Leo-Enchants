package com.leo.enchants.entity;

import com.leo.enchants.LeoEnchantsMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

/**
 * Herobrine - A legendary boss entity with 3 phases.
 * Each phase has 1000 hearts (2000 HP).
 * 
 * Phase 1: Basic attacks (obsidian, creepers)
 * Phase 2: Reality-breaking (gravity, blocks, force field, giant sword from sky)
 * Phase 3: World corruption (block/entity dissolution into 1s and 0s, screen glitch)
 */
public class HerobrineEntity extends HostileEntity {
    
    // Data trackers
    private static final TrackedData<Integer> PHASE = DataTracker.registerData(HerobrineEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> PHASE_HEALTH = DataTracker.registerData(HerobrineEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> IS_ATTACKING = DataTracker.registerData(HerobrineEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> ATTACK_TYPE = DataTracker.registerData(HerobrineEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> SWORD_SWING_TICKS = DataTracker.registerData(HerobrineEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> IS_SKY_ATTACK = DataTracker.registerData(HerobrineEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_DYING = DataTracker.registerData(HerobrineEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    
    // Constants
    public static final float PHASE_MAX_HEALTH = 2000.0f;
    public static final int MAX_PHASE = 3;
    private static final float MELEE_DAMAGE_REDUCTION = 0.30f;
    
    // Gravity modifier identifier
    private static final Identifier HEROBRINE_GRAVITY_ID = Identifier.of(LeoEnchantsMod.MOD_ID, "herobrine_gravity");
    
    // Attack cooldowns (in ticks)
    private static final int OBSIDIAN_ATTACK_COOLDOWN = 140; // 7 seconds
    private static final int SKY_SWORD_ATTACK_COOLDOWN = 200; // 10 seconds
    private static final int CREEPER_MISSILE_COOLDOWN = 200; // 10 seconds (Phase 1 only)
    private static final int DARKNESS_TP_COOLDOWN = 100; // 5 seconds
    private static final int FATAL_STRIKE_MIN_COOLDOWN = 600; // 30 seconds
    private static final int FATAL_STRIKE_MAX_COOLDOWN = 900; // 45 seconds
    
    // Phase 3 cooldowns
    private static final int EVAPORATION_MIN_COOLDOWN = 200; // 10 seconds
    private static final int EVAPORATION_MAX_COOLDOWN = 600; // 30 seconds
    private static final int INVENTORY_SHUFFLE_COOLDOWN = 100; // 5 seconds
    private static final int SCREEN_GLITCH_COOLDOWN = 40; // 2 seconds
    private static final int ENTITY_DISSOLUTION_INTERVAL = 40; // 2 seconds
    
    // Teleport timing (5-10 seconds)
    private static final int MIN_TELEPORT_INTERVAL = 100;
    private static final int MAX_TELEPORT_INTERVAL = 200;
    
    // Movement speed
    private static final float APPROACH_SPEED = 0.08f;
    private static final float PREFERRED_DISTANCE = 10.0f;
    
    // Phase 2 timers
    private static final int GRAVITY_CHANGE_INTERVAL = 60; // 3 seconds
    private static final int BLOCK_CORRUPTION_INTERVAL = 40; // 2 seconds
    private static final int FORCE_FIELD_STRENGTH_TICKS = 5;
    
    // Sky sword attack
    private static final int SKY_SWORD_SWING_DURATION = 60;
    private static final float SKY_ATTACK_HEIGHT = 25.0f;
    
    // Death animation
    private static final int DEATH_ANIMATION_DURATION = 200; // 10 seconds
    
    // Attack timers
    private int obsidianCooldown = 0;
    private int skySwordCooldown = 0;
    private int creeperMissileCooldown = 0;
    private int darknessTpCooldown = 0;
    private int teleportCooldown = 0;
    private int fatalStrikeCooldown = 0;
    private int gravityChangeCooldown = 0;
    private int blockCorruptionCooldown = 0;
    private int evaporationCooldown = 0;
    private int inventoryShuffleCooldown = 0;
    private int screenGlitchCooldown = 0;
    
    // Death animation
    private int deathAnimationTicks = 0;
    
    // Delayed action system
    private int fatalStrikeDelayTicks = 0;
    private LivingEntity fatalStrikeTarget = null;
    
    // Player look tracking
    private Map<UUID, Integer> playerLookTimers = new HashMap<>();
    private static final int LOOK_TIME_FOR_DARKNESS = 20;
    
    // Boss bar
    private final ServerBossBar bossBar;
    
    // Attack state
    private int attackAnimationTicks = 0;
    private LivingEntity currentTarget = null;
    
    // Sword swing state
    private Vec3d swordSwingDirection = null;
    private Vec3d skyAttackStartPos = null;
    
    // Gravity state per player
    private Map<UUID, Double> playerGravityModifier = new HashMap<>();
    
    public HerobrineEntity(EntityType<? extends HerobrineEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true);
        this.experiencePoints = 500;
        
        this.bossBar = new ServerBossBar(
            Text.literal("§4§lHerobrine"),
            BossBar.Color.RED,
            BossBar.Style.NOTCHED_10
        );
        
        this.teleportCooldown = MIN_TELEPORT_INTERVAL + random.nextInt(MAX_TELEPORT_INTERVAL - MIN_TELEPORT_INTERVAL);
        this.fatalStrikeCooldown = FATAL_STRIKE_MIN_COOLDOWN + random.nextInt(FATAL_STRIKE_MAX_COOLDOWN - FATAL_STRIKE_MIN_COOLDOWN);
        this.evaporationCooldown = EVAPORATION_MIN_COOLDOWN + random.nextInt(EVAPORATION_MAX_COOLDOWN - EVAPORATION_MIN_COOLDOWN);
    }
    
    public static DefaultAttributeContainer.Builder createHerobrineAttributes() {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.MAX_HEALTH, PHASE_MAX_HEALTH * MAX_PHASE)
            .add(EntityAttributes.MOVEMENT_SPEED, 0.0)
            .add(EntityAttributes.FLYING_SPEED, 0.0)
            .add(EntityAttributes.ATTACK_DAMAGE, 20.0)
            .add(EntityAttributes.ARMOR, 10.0)
            .add(EntityAttributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(EntityAttributes.FOLLOW_RANGE, 100.0);
    }
    
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(PHASE, 1);
        builder.add(PHASE_HEALTH, PHASE_MAX_HEALTH);
        builder.add(IS_ATTACKING, false);
        builder.add(ATTACK_TYPE, 0);
        builder.add(SWORD_SWING_TICKS, 0);
        builder.add(IS_SKY_ATTACK, false);
        builder.add(IS_DYING, false);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Handle death animation
        if (getIsDying()) {
            handleDeathAnimation();
            return;
        }
        
        // Decrease cooldowns
        if (obsidianCooldown > 0) obsidianCooldown--;
        if (skySwordCooldown > 0) skySwordCooldown--;
        if (creeperMissileCooldown > 0) creeperMissileCooldown--;
        if (darknessTpCooldown > 0) darknessTpCooldown--;
        if (attackAnimationTicks > 0) attackAnimationTicks--;
        if (fatalStrikeCooldown > 0) fatalStrikeCooldown--;
        if (gravityChangeCooldown > 0) gravityChangeCooldown--;
        if (blockCorruptionCooldown > 0) blockCorruptionCooldown--;
        if (evaporationCooldown > 0) evaporationCooldown--;
        if (inventoryShuffleCooldown > 0) inventoryShuffleCooldown--;
        if (screenGlitchCooldown > 0) screenGlitchCooldown--;
        
        if (getSwordSwingTicks() <= 0 && !getIsSkyAttack() && teleportCooldown > 0) {
            teleportCooldown--;
        }
        
        // Handle sky sword attack
        if (getIsSkyAttack() && getSwordSwingTicks() > 0) {
            handleSkyAttackAnimation();
        }
        
        if (getSwordSwingTicks() > 0) {
            setSwordSwingTicks(getSwordSwingTicks() - 1);
            if (getSwordSwingTicks() <= 0) {
                setIsAttacking(false);
                setIsSkyAttack(false);
                swordSwingDirection = null;
                skyAttackStartPos = null;
            }
        }
        
        if (!getWorld().isClient) {
            ServerWorld serverWorld = (ServerWorld) getWorld();
            
            // Handle delayed fatal strike
            if (fatalStrikeDelayTicks > 0) {
                fatalStrikeDelayTicks--;
                if (fatalStrikeDelayTicks == 0 && fatalStrikeTarget != null) {
                    executeFatalStrikeDamage(serverWorld, fatalStrikeTarget);
                    fatalStrikeTarget = null;
                }
            }
            
            updateBossBar();
            updateTarget(serverWorld);
            checkForDespawn(serverWorld);
            
            if (getSwordSwingTicks() <= 0 && !getIsSkyAttack()) {
                checkPlayerLooking(serverWorld);
                handleTeleportation(serverWorld);
                approachTarget(serverWorld);
            } else {
                setVelocity(0, 0, 0);
            }
            
            executePhaseAbilities(serverWorld);
            spawnAmbientParticles(serverWorld);
            spawnHorrorEffects(serverWorld);
            applyDarknessFilter(serverWorld);
            
            if (currentTarget != null && !getIsSkyAttack()) {
                lookAtEntity(currentTarget, 180.0f, 90.0f);
            }
        } else {
            setVelocity(0, 0, 0);
        }
    }
    
    /**
     * Handle the death animation with hexagonal expanding ring
     */
    private void handleDeathAnimation() {
        if (!getWorld().isClient && getWorld() instanceof ServerWorld world) {
            deathAnimationTicks++;
            
            float progress = (float) deathAnimationTicks / DEATH_ANIMATION_DURATION;
            float radius = progress * 50;
            
            // Draw hexagonal ring using particles
            int hexPoints = 6;
            int particlesPerSide = (int) (5 + radius / 2);
            
            for (int side = 0; side < hexPoints; side++) {
                double angle1 = (2 * Math.PI / hexPoints) * side;
                double angle2 = (2 * Math.PI / hexPoints) * ((side + 1) % hexPoints);
                
                double x1 = getX() + Math.cos(angle1) * radius;
                double z1 = getZ() + Math.sin(angle1) * radius;
                double x2 = getX() + Math.cos(angle2) * radius;
                double z2 = getZ() + Math.sin(angle2) * radius;
                
                for (int i = 0; i < particlesPerSide; i++) {
                    float t = (float) i / particlesPerSide;
                    double px = x1 + (x2 - x1) * t;
                    double pz = z1 + (z2 - z1) * t;
                    
                    world.spawnParticles(ParticleTypes.END_ROD, px, getY() + 1, pz, 1, 0, 0.1, 0, 0.01);
                    world.spawnParticles(ParticleTypes.FLASH, px, getY() + 1, pz, 1, 0, 0, 0, 0);
                }
            }
            
            // Inner glow effect
            if (deathAnimationTicks % 5 == 0) {
                for (int i = 0; i < 20; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double r = random.nextDouble() * radius * 0.5;
                    world.spawnParticles(ParticleTypes.END_ROD,
                        getX() + Math.cos(angle) * r,
                        getY() + 1 + random.nextDouble() * 3,
                        getZ() + Math.sin(angle) * r,
                        1, 0, 0.05, 0, 0.02);
                }
            }
            
            // Apply black and white effect to all players
            if (deathAnimationTicks % 10 == 0) {
                for (ServerPlayerEntity player : world.getPlayers()) {
                    if (player.distanceTo(this) < 100) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 5, 0, false, false));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 220, 0, false, false));
                    }
                }
            }
            
            // Ominous sounds
            if (deathAnimationTicks == 1) {
                world.playSound(null, getBlockPos(), SoundEvents.ENTITY_WITHER_DEATH, SoundCategory.HOSTILE, 2.0f, 0.3f);
                world.playSound(null, getBlockPos(), SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.HOSTILE, 2.0f, 0.5f);
            }
            
            if (deathAnimationTicks % 40 == 0 && deathAnimationTicks < DEATH_ANIMATION_DURATION) {
                world.playSound(null, getBlockPos(), SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.HOSTILE, 1.5f, 0.5f + progress * 0.5f);
            }
            
            // Final death
            if (deathAnimationTicks >= DEATH_ANIMATION_DURATION) {
                // Reset gravity for all players
                for (PlayerEntity player : world.getPlayers()) {
                    resetPlayerGravity((ServerPlayerEntity) player);
                }
                
                // Massive final explosion of particles
                for (int i = 0; i < 200; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double r = random.nextDouble() * 10;
                    world.spawnParticles(ParticleTypes.END_ROD,
                        getX() + Math.cos(angle) * r,
                        getY() + random.nextDouble() * 5,
                        getZ() + Math.sin(angle) * r,
                        1, 0, 0.2, 0, 0.1);
                }
                
                world.playSound(null, getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.HOSTILE, 2.0f, 0.5f);
                
                this.discard();
            }
        }
    }
    
    private void handleSkyAttackAnimation() {
        if (skyAttackStartPos == null || currentTarget == null) return;
        
        int totalTicks = SKY_SWORD_SWING_DURATION;
        int currentTick = totalTicks - getSwordSwingTicks();
        float progress = (float) currentTick / totalTicks;
        
        Vec3d targetPos = currentTarget.getPos().add(0, 2, 0);
        Vec3d currentPos = skyAttackStartPos.lerp(targetPos, progress);
        
        this.setPosition(currentPos.x, currentPos.y, currentPos.z);
        
        if (getSwordSwingTicks() <= 10 && getSwordSwingTicks() > 5) {
            performSkySwordDamage();
        }
        
        if (getWorld() instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY(), getZ(), 5, 0.3, 0.3, 0.3, 0.05);
            world.spawnParticles(ParticleTypes.SMOKE, getX(), getY() + 2, getZ(), 3, 0.2, 0.2, 0.2, 0.02);
        }
    }
    
    private void applyDarknessFilter(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isCreative() || player.isSpectator()) continue;
            double distance = player.distanceTo(this);
            if (distance < 50) {
                if (!player.hasStatusEffect(StatusEffects.DARKNESS)) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 60, 0, false, false));
                }
            }
        }
    }
    
    private void approachTarget(ServerWorld world) {
        if (currentTarget == null) {
            setVelocity(0, 0, 0);
            return;
        }
        
        double distanceToTarget = this.distanceTo(currentTarget);
        Vec3d targetPos = currentTarget.getPos().add(0, 3, 0);
        
        if (distanceToTarget > PREFERRED_DISTANCE + 5) {
            Vec3d direction = targetPos.subtract(getPos()).normalize();
            setVelocity(direction.multiply(APPROACH_SPEED));
        } else if (distanceToTarget < PREFERRED_DISTANCE - 3) {
            Vec3d direction = getPos().subtract(targetPos).normalize();
            setVelocity(direction.multiply(APPROACH_SPEED * 0.5));
        } else {
            double bob = Math.sin(age * 0.05) * 0.02;
            setVelocity(0, bob, 0);
        }
    }
    
    private void updateBossBar() {
        int phase = getPhase();
        float phaseHealth = getPhaseHealth();
        
        bossBar.setName(Text.literal("§4§lHerobrine §7[" + phase + "/3]"));
        bossBar.setPercent(phaseHealth / PHASE_MAX_HEALTH);
        
        bossBar.setColor(switch (phase) {
            case 1 -> BossBar.Color.RED;
            case 2 -> BossBar.Color.YELLOW;
            case 3 -> BossBar.Color.PURPLE;
            default -> BossBar.Color.RED;
        });
    }
    
    private void updateTarget(ServerWorld world) {
        PlayerEntity nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (PlayerEntity player : world.getPlayers()) {
            if (player.isSpectator() || player.isCreative() || !player.isAlive()) continue;
            double distance = player.squaredDistanceTo(this);
            if (distance < 10000 && distance < nearestDistance) {
                nearestDistance = distance;
                nearestPlayer = player;
            }
        }
        
        if (nearestPlayer != null) {
            this.currentTarget = nearestPlayer;
            this.setTarget(nearestPlayer);
        } else {
            this.currentTarget = null;
            this.setTarget(null);
        }
    }
    
    private void checkForDespawn(ServerWorld world) {
        boolean anyValidPlayer = false;
        
        for (PlayerEntity player : world.getPlayers()) {
            if (!player.isSpectator() && !player.isCreative() && player.isAlive()) {
                if (player.squaredDistanceTo(this) < 10000) {
                    anyValidPlayer = true;
                    break;
                }
            }
        }
        
        if (!anyValidPlayer) {
            for (PlayerEntity player : world.getPlayers()) {
                resetPlayerGravity((ServerPlayerEntity) player);
            }
            
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(), 50, 0.5, 1, 0.5, 0.1);
            world.playSound(null, getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.5f, 0.3f);
            this.discard();
        }
    }
    
    private void handleTeleportation(ServerWorld world) {
        if (teleportCooldown <= 0 && currentTarget != null) {
            performRandomTeleport(world);
            teleportCooldown = MIN_TELEPORT_INTERVAL + random.nextInt(MAX_TELEPORT_INTERVAL - MIN_TELEPORT_INTERVAL);
        }
    }
    
    private void performRandomTeleport(ServerWorld world) {
        if (currentTarget == null) return;
        
        double playerYaw = Math.toRadians(currentTarget.getYaw());
        double baseAngle = playerYaw + Math.PI;
        double angle = baseAngle + (random.nextDouble() - 0.5) * Math.PI;
        double distance = 6 + random.nextDouble() * 8;
        double heightOffset = 2 + random.nextDouble() * 4;
        
        double targetX = currentTarget.getX() + Math.cos(angle) * distance;
        double targetZ = currentTarget.getZ() + Math.sin(angle) * distance;
        
        BlockPos groundPos = BlockPos.ofFloored(targetX, currentTarget.getY() + 10, targetZ);
        while (world.getBlockState(groundPos).isAir() && groundPos.getY() > world.getBottomY()) {
            groundPos = groundPos.down();
        }
        
        double targetY = groundPos.getY() + heightOffset;
        
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(), 40, 0.5, 1, 0.5, 0.15);
        for (int i = 0; i < 16; i++) {
            double a = (2 * Math.PI / 16) * i;
            double r = 1.5;
            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, getX() + Math.cos(a) * r, getY() + 1, getZ() + Math.sin(a) * r, 2, 0.05, 0.2, 0.05, 0.03);
        }
        world.spawnParticles(ParticleTypes.SOUL, getX(), getY(), getZ(), 15, 0.3, 0.5, 0.3, 0.05);
        world.playSound(null, getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.2f, 0.4f);
        
        this.teleport(targetX, targetY, targetZ, false);
        
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(), 40, 0.5, 1, 0.5, 0.15);
        for (int ring = 0; ring < 3; ring++) {
            double radius = 0.5 + ring * 0.5;
            for (int i = 0; i < 12; i++) {
                double a = (2 * Math.PI / 12) * i;
                world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, getX() + Math.cos(a) * radius, getY(), getZ() + Math.sin(a) * radius, 1, 0, 0.1, 0, 0.02);
            }
        }
        world.playSound(null, getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.2f, 0.4f);
    }
    
    private void checkPlayerLooking(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isCreative() || player.isSpectator()) continue;
            if (player.squaredDistanceTo(this) > 2500) continue;
            
            if (isPlayerLookingAtMe(player)) {
                UUID playerId = player.getUuid();
                int lookTime = playerLookTimers.getOrDefault(playerId, 0) + 1;
                playerLookTimers.put(playerId, lookTime);
                
                if (lookTime >= LOOK_TIME_FOR_DARKNESS && darknessTpCooldown <= 0) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 80, 1, false, false));
                    teleportBehindPlayer(world, player);
                    darknessTpCooldown = DARKNESS_TP_COOLDOWN;
                    playerLookTimers.put(playerId, 0);
                }
            } else {
                playerLookTimers.put(player.getUuid(), 0);
            }
        }
    }
    
    private boolean isPlayerLookingAtMe(PlayerEntity player) {
        Vec3d playerLook = player.getRotationVector();
        Vec3d toHerobrine = this.getPos().add(0, this.getHeight() / 2, 0).subtract(player.getEyePos()).normalize();
        double dot = playerLook.dotProduct(toHerobrine);
        return dot > 0.95;
    }
    
    private void teleportBehindPlayer(ServerWorld world, PlayerEntity player) {
        Vec3d playerLook = player.getRotationVector();
        Vec3d behindPlayer = player.getPos().subtract(playerLook.multiply(3));
        
        BlockPos targetPos = BlockPos.ofFloored(behindPlayer);
        while (!world.getBlockState(targetPos.down()).isSolidBlock(world, targetPos.down()) && targetPos.getY() > world.getBottomY()) {
            targetPos = targetPos.down();
        }
        
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(), 30, 0.5, 1, 0.5, 0.1);
        world.playSound(null, getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.0f, 0.5f);
        
        this.teleport(behindPlayer.x, targetPos.getY() + 1, behindPlayer.z, false);
        
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(), 30, 0.5, 1, 0.5, 0.1);
        world.playSound(null, getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.0f, 0.5f);
    }
    
    private void executePhaseAbilities(ServerWorld world) {
        if (currentTarget == null) return;
        if (currentTarget instanceof PlayerEntity player && player.isCreative()) return;
        
        int phase = getPhase();
        
        if (phase == 1) {
            executePhase1Abilities(world);
        }
        
        if (phase == 2) {
            executePhase2Abilities(world);
        }
        
        if (phase == 3) {
            executePhase3Abilities(world);
        }
    }
    
    private void executePhase1Abilities(ServerWorld world) {
        if (currentTarget == null) return;
        
        double distanceToTarget = this.distanceTo(currentTarget);
        
        if (obsidianCooldown <= 0 && distanceToTarget > 5 && distanceToTarget < 40) {
            summonFloatingObsidians(world, currentTarget);
            obsidianCooldown = OBSIDIAN_ATTACK_COOLDOWN;
        }
        
        // Creepers - ONLY in Phase 1
        if (creeperMissileCooldown <= 0 && distanceToTarget < 50) {
            summonCreeperMissiles(world, currentTarget);
            creeperMissileCooldown = CREEPER_MISSILE_COOLDOWN;
        }
    }
    
    private void executePhase2Abilities(ServerWorld world) {
        if (currentTarget == null) return;
        
        double distanceToTarget = this.distanceTo(currentTarget);
        
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isCreative() || player.isSpectator()) continue;
            if (player.distanceTo(this) < 50) {
                player.sendMessage(Text.literal("§c§l⚠ WARNING: §r§7Herobrine is modifying constants..."), true);
            }
        }
        
        if (obsidianCooldown <= 0 && distanceToTarget > 5 && distanceToTarget < 40) {
            summonFloatingObsidians(world, currentTarget);
            obsidianCooldown = OBSIDIAN_ATTACK_COOLDOWN;
        }
        
        if (skySwordCooldown <= 0 && getSwordSwingTicks() <= 0 && !getIsSkyAttack() && distanceToTarget < 30) {
            performSkySwordAttack(world, currentTarget);
            skySwordCooldown = SKY_SWORD_ATTACK_COOLDOWN;
        }
        
        if (gravityChangeCooldown <= 0) {
            manipulateGravityAttribute(world);
            gravityChangeCooldown = GRAVITY_CHANGE_INTERVAL;
        }
        
        if (blockCorruptionCooldown <= 0) {
            corruptNearbyBlocks(world);
            blockCorruptionCooldown = BLOCK_CORRUPTION_INTERVAL;
        }
        
        if (age % FORCE_FIELD_STRENGTH_TICKS == 0) {
            applyForceField(world);
        }
        
        if (fatalStrikeCooldown <= 0) {
            performFatalStrike(world);
            fatalStrikeCooldown = FATAL_STRIKE_MIN_COOLDOWN + random.nextInt(FATAL_STRIKE_MAX_COOLDOWN - FATAL_STRIKE_MIN_COOLDOWN);
        }
    }
    
    /**
     * Phase 3: World corruption
     * - Blocks dissolve into 1s and 0s
     * - All entities (except players) within 10 blocks dissolve into 1s and 0s
     * - Red screen glitch effect (visual)
     * - NO creepers, NO constant modification
     */
    private void executePhase3Abilities(ServerWorld world) {
        if (currentTarget == null) return;
        
        double distanceToTarget = this.distanceTo(currentTarget);
        
        // Reset gravity for all players in Phase 3
        for (ServerPlayerEntity player : world.getPlayers()) {
            resetPlayerGravity(player);
        }
        
        // Obsidian attack - faster
        if (obsidianCooldown <= 0 && distanceToTarget > 5 && distanceToTarget < 40) {
            summonFloatingObsidians(world, currentTarget);
            obsidianCooldown = OBSIDIAN_ATTACK_COOLDOWN / 2;
        }
        
        // Sky Sword Attack - faster
        if (skySwordCooldown <= 0 && getSwordSwingTicks() <= 0 && !getIsSkyAttack() && distanceToTarget < 30) {
            performSkySwordAttack(world, currentTarget);
            skySwordCooldown = SKY_SWORD_ATTACK_COOLDOWN / 2;
        }
        
        // Force field - stronger
        if (age % 3 == 0) {
            applyForceField(world);
        }
        
        // Fatal Strike - faster
        if (fatalStrikeCooldown <= 0) {
            performFatalStrike(world);
            fatalStrikeCooldown = (FATAL_STRIKE_MIN_COOLDOWN + random.nextInt(FATAL_STRIKE_MAX_COOLDOWN - FATAL_STRIKE_MIN_COOLDOWN)) / 2;
        }
        
        // ====== PHASE 3 EXCLUSIVE ABILITIES ======
        
        // Block Evaporation into 1s and 0s - every 30-60 seconds
        if (evaporationCooldown <= 0) {
            evaporateBlocksIntoBinary(world);
            evaporationCooldown = EVAPORATION_MIN_COOLDOWN + random.nextInt(EVAPORATION_MAX_COOLDOWN - EVAPORATION_MIN_COOLDOWN);
        }
        
        // Inventory Shuffle - every 5 seconds
        if (inventoryShuffleCooldown <= 0) {
            shufflePlayerInventory(world);
            inventoryShuffleCooldown = INVENTORY_SHUFFLE_COOLDOWN;
        }
        
        // Red Screen Glitch Effect - every 2 seconds
        if (screenGlitchCooldown <= 0) {
            applyScreenGlitchEffect(world);
            screenGlitchCooldown = SCREEN_GLITCH_COOLDOWN;
        }
        
        // Dissolve all entities (except players) within 10 blocks into 1s and 0s
        if (age % ENTITY_DISSOLUTION_INTERVAL == 0) {
            dissolveNearbyEntities(world);
        }
    }
    
    /**
     * Evaporate blocks with enchant particle effect (lightweight 1s and 0s visual)
     * Only removes 5-15 random blocks to prevent lag
     */
    private void evaporateBlocksIntoBinary(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isCreative() || player.isSpectator()) continue;
            if (player.distanceTo(this) > 50) continue;
            
            int radius = 6;
            List<BlockPos> validBlocks = new ArrayList<>();
            
            BlockPos playerPos = player.getBlockPos();
            // Find valid blocks
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos pos = playerPos.add(x, y, z);
                        double distance = Math.sqrt(x*x + y*y + z*z);
                        
                        if (distance <= radius && !world.getBlockState(pos).isAir()) {
                            if (world.getBlockState(pos).getHardness(world, pos) >= 0) {
                                validBlocks.add(pos);
                            }
                        }
                    }
                }
            }
            
            // Only remove 5-15 random blocks to prevent lag
            int blocksToRemove = Math.min(validBlocks.size(), 5 + random.nextInt(11));
            Collections.shuffle(validBlocks);
            
            for (int i = 0; i < blocksToRemove; i++) {
                BlockPos pos = validBlocks.get(i);
                
                // Remove the block
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
                
                // Use ENCHANT particles - looks like floating symbols/letters (1s and 0s effect)
                // These rise up naturally like the enchanting table effect
                world.spawnParticles(ParticleTypes.ENCHANT,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    15, 0.4, 0.4, 0.4, 0.5);
                
                // Add some end rod particles for the "digital" dissolve effect
                world.spawnParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    3, 0.2, 0.2, 0.2, 0.1);
            }
            
            if (blocksToRemove > 0) {
                world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.HOSTILE, 1.0f, 2.0f);
            }
        }
    }
    
    /**
     * Apply red screen glitch effect - visual only, no actual blocks
     */
    private void applyScreenGlitchEffect(ServerWorld world) {
        // Red dust particles for the "hacked" visual effect
        DustParticleEffect redDust = new DustParticleEffect(0xFF0000, 2.0f);
        
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isCreative() || player.isSpectator()) continue;
            if (player.distanceTo(this) > 50) continue;
            
            Vec3d look = player.getRotationVector();
            Vec3d right = look.crossProduct(new Vec3d(0, 1, 0)).normalize();
            Vec3d up = right.crossProduct(look).normalize();
            
            // Spawn red particles in front of player's face (screen glitch effect)
            for (int i = 0; i < 15; i++) {
                double distance = 1.5 + random.nextDouble() * 0.5;
                double xOffset = (random.nextDouble() - 0.5) * 2;
                double yOffset = (random.nextDouble() - 0.5) * 1.5;
                
                Vec3d particlePos = player.getEyePos()
                    .add(look.multiply(distance))
                    .add(right.multiply(xOffset))
                    .add(up.multiply(yOffset));
                
                // Red glitch particles
                world.spawnParticles(redDust,
                    particlePos.x, particlePos.y, particlePos.z,
                    1, 0.1, 0.1, 0.1, 0);
            }
            
            // Additional red particle bursts at random screen positions
            for (int i = 0; i < 5; i++) {
                double distance = 2 + random.nextDouble() * 2;
                double angle = random.nextDouble() * Math.PI * 2;
                
                Vec3d burstPos = player.getEyePos()
                    .add(look.multiply(distance))
                    .add(right.multiply(Math.cos(angle) * 1.5))
                    .add(up.multiply(Math.sin(angle) * 1.5));
                
                world.spawnParticles(redDust,
                    burstPos.x, burstPos.y, burstPos.z,
                    3, 0.2, 0.2, 0.2, 0);
            }
            
            // Brief damage flash for red vignette
            if (random.nextInt(3) == 0) {
                player.damage(world, world.getDamageSources().magic(), 0.1f);
            }
            
            // Glitch sounds
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_SCULK_BREAK, SoundCategory.HOSTILE, 0.3f, 0.5f);
        }
    }
    
    /**
     * Dissolve all entities (mobs, items) within 10 blocks of Herobrine into 1s and 0s
     * Uses enchant particles for the "digital dissolve" effect
     */
    private void dissolveNearbyEntities(ServerWorld world) {
        Box dissolutionArea = getBoundingBox().expand(10);
        
        for (Entity entity : world.getOtherEntities(this, dissolutionArea)) {
            // Skip players and Herobrine's own projectiles
            if (entity instanceof PlayerEntity) continue;
            if (entity instanceof HerobrineObsidianEntity) continue;
            
            Vec3d entityPos = entity.getPos();
            
            // Enchant particles - looks like floating 1s and 0s rising
            world.spawnParticles(ParticleTypes.ENCHANT,
                entityPos.x, entityPos.y + entity.getHeight() / 2, entityPos.z,
                20, entity.getWidth() / 2, entity.getHeight() / 2, entity.getWidth() / 2, 0.5);
            
            // End rod particles for dissolve effect
            world.spawnParticles(ParticleTypes.END_ROD,
                entityPos.x, entityPos.y + entity.getHeight() / 2, entityPos.z,
                5, 0.2, 0.2, 0.2, 0.1);
            
            // Remove the entity
            entity.discard();
            
            // Glitch sound
            world.playSound(null, entity.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 0.5f, 2.0f);
        }
    }
    
    private void shufflePlayerInventory(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isCreative() || player.isSpectator()) continue;
            if (player.distanceTo(this) > 40) continue;
            
            PlayerInventory inventory = player.getInventory();
            
            int swapCount = 3 + random.nextInt(5);
            for (int i = 0; i < swapCount; i++) {
                int slot1 = random.nextInt(36);
                int slot2 = random.nextInt(36);
                
                if (slot1 != slot2) {
                    ItemStack stack1 = inventory.getStack(slot1);
                    ItemStack stack2 = inventory.getStack(slot2);
                    
                    inventory.setStack(slot1, stack2);
                    inventory.setStack(slot2, stack1);
                }
            }
            
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                player.getX(), player.getY() + 1, player.getZ(),
                20, 0.3, 0.5, 0.3, 0.05);
            
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_SHULKER_TELEPORT, SoundCategory.HOSTILE, 0.5f, 1.5f);
        }
    }
    
    private void performSkySwordAttack(ServerWorld world, LivingEntity target) {
        skyAttackStartPos = new Vec3d(target.getX(), target.getY() + SKY_ATTACK_HEIGHT, target.getZ());
        
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(), 30, 0.5, 1, 0.5, 0.1);
        world.playSound(null, getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.5f, 0.3f);
        
        this.teleport(skyAttackStartPos.x, skyAttackStartPos.y, skyAttackStartPos.z, false);
        
        setIsAttacking(true);
        setIsSkyAttack(true);
        setAttackType(2);
        setSwordSwingTicks(SKY_SWORD_SWING_DURATION);
        attackAnimationTicks = SKY_SWORD_SWING_DURATION;
        
        swordSwingDirection = target.getPos().subtract(getPos()).normalize();
        
        world.playSound(null, getBlockPos(), SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 2.0f, 0.5f);
        world.playSound(null, getBlockPos(), SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.HOSTILE, 1.0f, 0.3f);
        
        for (int i = 0; i < 20; i++) {
            double angle = (2 * Math.PI / 20) * i;
            double radius = 4;
            world.spawnParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                target.getX() + Math.cos(angle) * radius,
                target.getY() + 0.1,
                target.getZ() + Math.sin(angle) * radius,
                2, 0, 0.1, 0, 0.02);
        }
    }
    
    private void performSkySwordDamage() {
        if (!(getWorld() instanceof ServerWorld world)) return;
        
        world.playSound(null, getBlockPos(), SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.HOSTILE, 2.0f, 0.3f);
        world.playSound(null, getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.HOSTILE, 2.0f, 0.5f);
        world.playSound(null, getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.HOSTILE, 1.0f, 0.5f);
        
        double hitRange = 10;
        Box damageArea = getBoundingBox().expand(hitRange);
        
        for (Entity entity : world.getOtherEntities(this, damageArea)) {
            if (entity instanceof ServerPlayerEntity player) {
                if (player.isCreative() || player.isSpectator()) continue;
                
                float maxDamage = player.getMaxHealth() * 0.8f;
                float actualDamage = Math.min(maxDamage, player.getHealth() - 1);
                
                if (actualDamage > 0) {
                    player.damage(world, world.getDamageSources().magic(), actualDamage);
                    
                    Vec3d knockback = player.getPos().subtract(getPos()).normalize().multiply(3);
                    player.addVelocity(knockback.x, 1.0, knockback.z);
                    
                    damagePlayerArmor(player);
                }
            }
        }
        
        for (int i = 0; i < 100; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = random.nextDouble() * hitRange;
            world.spawnParticles(ParticleTypes.EXPLOSION, getX() + Math.cos(angle) * radius, getY() + 0.5, getZ() + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
        }
        
        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 3, 0, 0, 0, 0);
        
        for (int i = 0; i < 32; i++) {
            double angle = (2 * Math.PI / 32) * i;
            double radius = 8;
            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, getX() + Math.cos(angle) * radius, getY() + 0.5, getZ() + Math.sin(angle) * radius, 3, 0, 0.2, 0, 0.05);
        }
    }
    
    private void manipulateGravityAttribute(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isCreative() || player.isSpectator()) continue;
            if (player.distanceTo(this) > 40) continue;
            
            EntityAttributeInstance gravityAttr = player.getAttributeInstance(EntityAttributes.GRAVITY);
            if (gravityAttr == null) continue;
            
            gravityAttr.removeModifier(HEROBRINE_GRAVITY_ID);
            
            double gravityMod = (random.nextDouble() - 0.5) * 0.12;
            playerGravityModifier.put(player.getUuid(), gravityMod);
            
            EntityAttributeModifier modifier = new EntityAttributeModifier(
                HEROBRINE_GRAVITY_ID,
                gravityMod,
                EntityAttributeModifier.Operation.ADD_VALUE
            );
            gravityAttr.addTemporaryModifier(modifier);
            
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 10, 0.3, 0.5, 0.3, 0.05);
        }
        
        world.playSound(null, getBlockPos(), SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.HOSTILE, 0.5f, 0.3f);
    }
    
    private void resetPlayerGravity(ServerPlayerEntity player) {
        EntityAttributeInstance gravityAttr = player.getAttributeInstance(EntityAttributes.GRAVITY);
        if (gravityAttr != null) {
            gravityAttr.removeModifier(HEROBRINE_GRAVITY_ID);
        }
        playerGravityModifier.remove(player.getUuid());
    }
    
    private void corruptNearbyBlocks(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isCreative() || player.isSpectator()) continue;
            if (player.distanceTo(this) > 30) continue;
            
            int range = 3;
            int x = (int) player.getX() + random.nextInt(range * 2 + 1) - range;
            int y = (int) player.getY() + random.nextInt(3) - 1;
            int z = (int) player.getZ() + random.nextInt(range * 2 + 1) - range;
            
            BlockPos pos = new BlockPos(x, y, z);
            BlockState currentState = world.getBlockState(pos);
            
            if (currentState.isAir() || currentState.isReplaceable()) {
                int blockType = random.nextInt(5);
                BlockState newState = switch (blockType) {
                    case 0 -> Blocks.FIRE.getDefaultState();
                    case 1 -> Blocks.SOUL_FIRE.getDefaultState();
                    case 2 -> Blocks.MAGMA_BLOCK.getDefaultState();
                    case 3 -> Blocks.COBWEB.getDefaultState();
                    case 4 -> Blocks.POWDER_SNOW.getDefaultState();
                    default -> null;
                };
                
                if (newState != null) {
                    world.setBlockState(pos, newState);
                    world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, x + 0.5, y + 0.5, z + 0.5, 5, 0.2, 0.2, 0.2, 0.02);
                }
            }
        }
    }
    
    private void applyForceField(ServerWorld world) {
        int phase = getPhase();
        double forceStrength = phase == 3 ? 0.15 : 0.08;
        double forceRange = phase == 3 ? 15 : 10;
        
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isCreative() || player.isSpectator()) continue;
            double distance = player.distanceTo(this);
            
            if (distance < forceRange && distance > 0.1) {
                Vec3d pushDirection = player.getPos().subtract(getPos()).normalize();
                double forceFactor = 1.0 - (distance / forceRange);
                player.addVelocity(
                    pushDirection.x * forceStrength * forceFactor,
                    0.02 * forceFactor,
                    pushDirection.z * forceStrength * forceFactor
                );
            }
        }
    }
    
    private void performFatalStrike(ServerWorld world) {
        if (currentTarget == null) return;
        
        world.playSound(null, getBlockPos(), SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 2.0f, 0.3f);
        world.playSound(null, getBlockPos(), SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 1.5f, 0.5f);
        
        for (int i = 0; i < 30; i++) {
            double angle = (2 * Math.PI / 30) * i;
            double radius = 5;
            world.spawnParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                currentTarget.getX() + Math.cos(angle) * radius,
                currentTarget.getY() + 1,
                currentTarget.getZ() + Math.sin(angle) * radius,
                3, 0, 0.1, 0, 0.05);
        }
        
        fatalStrikeDelayTicks = 10;
        fatalStrikeTarget = currentTarget;
    }
    
    private void executeFatalStrikeDamage(ServerWorld world, LivingEntity target) {
        if (target instanceof ServerPlayerEntity player && player.isAlive() && !player.isCreative()) {
            float damage = player.getMaxHealth() * 0.8f;
            float actualDamage = Math.min(damage, player.getHealth() - 1);
            
            if (actualDamage > 0) {
                player.damage(world, world.getDamageSources().magic(), actualDamage);
                
                world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
                world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.HOSTILE, 1.0f, 0.5f);
                
                Vec3d knockback = player.getPos().subtract(getPos()).normalize().multiply(2);
                player.addVelocity(knockback.x, 1.0, knockback.z);
            }
        }
    }
    
    public void damagePlayerArmor(ServerPlayerEntity player) {
        if (getPhase() < 2) return;
        
        EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (EquipmentSlot slot : armorSlots) {
            ItemStack armorPiece = player.getEquippedStack(slot);
            if (armorPiece.isEmpty() || !armorPiece.isDamageable()) continue;
            
            int maxDamage = armorPiece.getMaxDamage();
            int damageAmount = Math.max(1, maxDamage / 100);
            
            armorPiece.setDamage(armorPiece.getDamage() + damageAmount);
            
            if (armorPiece.getDamage() >= maxDamage) {
                player.equipStack(slot, ItemStack.EMPTY);
            }
        }
    }
    
    private void summonFloatingObsidians(ServerWorld world, LivingEntity target) {
        int obsidianCount = 3 + random.nextInt(3);
        
        world.playSound(null, getBlockPos(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.HOSTILE, 1.5f, 0.5f);
        
        for (int i = 0; i < obsidianCount; i++) {
            double angle = (2 * Math.PI / obsidianCount) * i + random.nextDouble() * 0.5;
            double radius = 2 + random.nextDouble() * 2;
            
            Vec3d spawnPos = getPos().add(Math.cos(angle) * radius, 1 + random.nextDouble() * 3, Math.sin(angle) * radius);
            
            HerobrineObsidianEntity obsidian = new HerobrineObsidianEntity(world, this, target, spawnPos);
            obsidian.setDelay(i * 5);
            world.spawnEntity(obsidian);
        }
    }
    
    private void summonCreeperMissiles(ServerWorld world, LivingEntity target) {
        int missileCount = 4 + random.nextInt(3);
        
        world.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_CREEPER_PRIMED, SoundCategory.HOSTILE, 2.0f, 0.8f);
        
        for (int i = 0; i < missileCount; i++) {
            double angle = (2 * Math.PI / missileCount) * i;
            double radius = 6 + random.nextDouble() * 4;
            
            double spawnX = target.getX() + Math.cos(angle) * radius;
            double spawnZ = target.getZ() + Math.sin(angle) * radius;
            double spawnY = target.getY() + 5 + random.nextDouble() * 3;
            
            BlockPos groundPos = BlockPos.ofFloored(spawnX, spawnY, spawnZ);
            while (world.getBlockState(groundPos).isAir() && groundPos.getY() > world.getBottomY()) {
                groundPos = groundPos.down();
            }
            
            CreeperEntity creeper = EntityType.CREEPER.create(world, SpawnReason.MOB_SUMMONED);
            if (creeper != null) {
                creeper.setPosition(spawnX, groundPos.getY() + 1, spawnZ);
                creeper.setTarget(target);
                creeper.setFuseSpeed(1);
                world.spawnEntity(creeper);
                world.spawnParticles(ParticleTypes.FLAME, spawnX, groundPos.getY() + 1, spawnZ, 20, 0.3, 0.5, 0.3, 0.05);
            }
        }
    }
    
    private void spawnAmbientParticles(ServerWorld world) {
        if (random.nextInt(2) == 0) {
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX() + (random.nextDouble() - 0.5) * 2, getY() + random.nextDouble() * 2.5, getZ() + (random.nextDouble() - 0.5) * 2, 2, 0.1, 0.1, 0.1, 0.02);
        }
        
        if (random.nextInt(4) == 0) {
            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, getX() + (random.nextDouble() - 0.5) * 1.5, getY() + random.nextDouble() * 2, getZ() + (random.nextDouble() - 0.5) * 1.5, 2, 0.05, 0.1, 0.05, 0.02);
        }
        
        if (random.nextInt(8) == 0) {
            world.spawnParticles(ParticleTypes.SOUL, getX() + (random.nextDouble() - 0.5) * 2, getY() - 0.5, getZ() + (random.nextDouble() - 0.5) * 2, 1, 0, 0.1, 0, 0.01);
        }
        
        if (random.nextInt(5) == 0) {
            world.spawnParticles(ParticleTypes.ASH, getX() + (random.nextDouble() - 0.5) * 3, getY() + random.nextDouble() * 2, getZ() + (random.nextDouble() - 0.5) * 3, 3, 0.2, 0.2, 0.2, 0.01);
        }
    }
    
    private void spawnHorrorEffects(ServerWorld world) {
        if (random.nextInt(10) == 0) {
            world.spawnParticles(ParticleTypes.DRIPPING_OBSIDIAN_TEAR, getX() + (random.nextDouble() - 0.5), getY(), getZ() + (random.nextDouble() - 0.5), 1, 0, 0, 0, 0);
        }
        
        if (random.nextInt(15) == 0) {
            world.spawnParticles(ParticleTypes.WARPED_SPORE, getX() + (random.nextDouble() - 0.5) * 4, getY() + random.nextDouble() * 3, getZ() + (random.nextDouble() - 0.5) * 4, 5, 0.5, 0.5, 0.5, 0.02);
        }
        
        if (age % 20 == 0) {
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI / 8) * i;
                double radius = 1.2;
                world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, getX() + Math.cos(angle) * radius, getY(), getZ() + Math.sin(angle) * radius, 1, 0, 0.05, 0, 0.01);
            }
        }
        
        if (random.nextInt(200) == 0) {
            world.playSound(null, getBlockPos(), SoundEvents.AMBIENT_CAVE.value(), SoundCategory.HOSTILE, 0.5f, 0.5f);
        }
        if (random.nextInt(400) == 0) {
            world.playSound(null, getBlockPos(), SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value(), SoundCategory.HOSTILE, 0.3f, 0.8f);
        }
    }
    
    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        if (getIsDying()) return false;
        
        if (source.getSource() instanceof PersistentProjectileEntity || source.getSource() instanceof ArrowEntity) {
            world.spawnParticles(ParticleTypes.CRIT, getX(), getY() + getHeight() / 2, getZ(), 10, 0.3, 0.3, 0.3, 0.1);
            world.playSound(null, getBlockPos(), SoundEvents.ITEM_SHIELD_BLOCK.value(), SoundCategory.HOSTILE, 1.0f, 1.5f);
            return false;
        }
        
        if (source.isOf(DamageTypes.PLAYER_ATTACK) || source.isOf(DamageTypes.MOB_ATTACK)) {
            amount *= (1.0f - MELEE_DAMAGE_REDUCTION);
        }
        
        float currentPhaseHealth = getPhaseHealth();
        float newPhaseHealth = currentPhaseHealth - amount;
        
        if (newPhaseHealth <= 0) {
            int currentPhase = getPhase();
            if (currentPhase < MAX_PHASE) {
                setPhase(currentPhase + 1);
                setPhaseHealth(PHASE_MAX_HEALTH);
                onPhaseTransition(world, currentPhase + 1);
                return true;
            } else {
                setPhaseHealth(0);
                setIsDying(true);
                deathAnimationTicks = 0;
                return true;
            }
        } else {
            setPhaseHealth(newPhaseHealth);
            playHurtSound(source);
            return true;
        }
    }
    
    private void onPhaseTransition(ServerWorld world, int newPhase) {
        world.playSound(null, getBlockPos(), SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 2.0f, 0.3f);
        world.playSound(null, getBlockPos(), SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 1.5f, 0.5f);
        world.playSound(null, getBlockPos(), SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.HOSTILE, 1.0f, 0.5f);
        
        for (int ring = 0; ring < 5; ring++) {
            double ringRadius = 2 + ring * 2;
            int particlesInRing = 16 + ring * 8;
            for (int i = 0; i < particlesInRing; i++) {
                double angle = (2 * Math.PI / particlesInRing) * i;
                world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, getX() + Math.cos(angle) * ringRadius, getY() + 0.5, getZ() + Math.sin(angle) * ringRadius, 3, 0.1, 0.3, 0.1, 0.05);
            }
        }
        
        for (int pillar = 0; pillar < 8; pillar++) {
            double angle = (2 * Math.PI / 8) * pillar;
            double radius = 6;
            double px = getX() + Math.cos(angle) * radius;
            double pz = getZ() + Math.sin(angle) * radius;
            
            for (int y = 0; y < 20; y++) {
                world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, px, getY() + y * 0.5, pz, 2, 0.1, 0.1, 0.1, 0.02);
                world.spawnParticles(ParticleTypes.SOUL, px, getY() + y * 0.5, pz, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }
        
        for (int i = 0; i < 150; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = random.nextDouble() * 8;
            double height = random.nextDouble() * 5;
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX() + Math.cos(angle) * radius, getY() + height, getZ() + Math.sin(angle) * radius, 1, 0.2, 0.2, 0.2, 0.08);
        }
        
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isCreative() || player.isSpectator()) continue;
            double distance = player.distanceTo(this);
            
            if (distance < 30) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 100, 1, false, false));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1, false, false));
                
                Vec3d knockback = player.getPos().subtract(getPos()).normalize().multiply(3);
                player.addVelocity(knockback.x, 1.5, knockback.z);
            }
        }
        
        LeoEnchantsMod.LOGGER.info("Herobrine transitioned to Phase {}", newPhase);
    }
    
    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        this.bossBar.addPlayer(player);
    }
    
    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossBar.removePlayer(player);
        resetPlayerGravity(player);
    }
    
    @Override
    public boolean canHaveStatusEffect(StatusEffectInstance effect) {
        return effect.getEffectType() != StatusEffects.POISON &&
               effect.getEffectType() != StatusEffects.WITHER &&
               effect.getEffectType() != StatusEffects.INSTANT_DAMAGE;
    }
    
    @Override
    public boolean isPushable() { return false; }
    
    @Override
    protected void pushAway(Entity entity) {}
    
    @Override
    public boolean hasNoGravity() { return true; }
    
    // Getters and Setters
    public int getPhase() { return this.dataTracker.get(PHASE); }
    public void setPhase(int phase) { this.dataTracker.set(PHASE, MathHelper.clamp(phase, 1, MAX_PHASE)); }
    public float getPhaseHealth() { return this.dataTracker.get(PHASE_HEALTH); }
    public void setPhaseHealth(float health) { this.dataTracker.set(PHASE_HEALTH, MathHelper.clamp(health, 0, PHASE_MAX_HEALTH)); }
    public boolean getIsAttacking() { return this.dataTracker.get(IS_ATTACKING); }
    public void setIsAttacking(boolean attacking) { this.dataTracker.set(IS_ATTACKING, attacking); }
    public int getAttackType() { return this.dataTracker.get(ATTACK_TYPE); }
    public void setAttackType(int type) { this.dataTracker.set(ATTACK_TYPE, type); }
    public int getSwordSwingTicks() { return this.dataTracker.get(SWORD_SWING_TICKS); }
    public void setSwordSwingTicks(int ticks) { this.dataTracker.set(SWORD_SWING_TICKS, ticks); }
    public boolean getIsSkyAttack() { return this.dataTracker.get(IS_SKY_ATTACK); }
    public void setIsSkyAttack(boolean skyAttack) { this.dataTracker.set(IS_SKY_ATTACK, skyAttack); }
    public boolean getIsDying() { return this.dataTracker.get(IS_DYING); }
    public void setIsDying(boolean dying) { this.dataTracker.set(IS_DYING, dying); }
    public int getAttackAnimationTicks() { return attackAnimationTicks; }
    public int getDeathAnimationTicks() { return deathAnimationTicks; }
}
