package com.leo.enchants.logic;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

import java.util.Collections;
import java.util.List;

public class WitherImpactLogic {

    public static void activate(PlayerEntity player, World world, int level) {
        if (world.isClient) return;

        // 1. Teleport
        boolean teleported = teleport(player, world);
        
        // 2. Explosion & Damage (with durability reduction)
        if (teleported) {
            createExplosion(player, world, level);
            reduceDurability(player, level);
        }

        // 3. Absorption
        applyAbsorption(player, level);
    }

    private static boolean teleport(PlayerEntity player, World world) {
        Vec3d eyePos = player.getCameraPosVec(1.0f);
        Vec3d lookVec = player.getRotationVector();
        double maxDistance = 8.0;
        
        Vec3d endPos = eyePos.add(lookVec.multiply(maxDistance));
        
        // Raycast to find the first solid block
        BlockHitResult hitResult = world.raycast(new RaycastContext(
            eyePos,
            endPos,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            player
        ));
        
        Vec3d targetPos = null;
        
        if (hitResult.getType() == HitResult.Type.MISS) {
            // No block hit - teleport to max distance in air
            targetPos = findValidAirPosition(world, eyePos, lookVec, maxDistance);
        } else {
            // Block hit - teleport to block surface
            BlockPos hitBlockPos = hitResult.getBlockPos();
            targetPos = getSurfacePosition(world, hitBlockPos, hitResult.getSide());
        }
        
        if (targetPos == null) {
            // No valid position found
            return false;
        }
        
        // Teleport the player
        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.teleport((ServerWorld) world, targetPos.x, targetPos.y, targetPos.z, 
                Collections.emptySet(), player.getYaw(), player.getPitch(), false);
            
            // Grant 3 seconds of fall damage immunity
            FallDamageImmunity.grantImmunity(player, world.getTime());
        } else {
            player.teleport(targetPos.x, targetPos.y, targetPos.z, true);
            FallDamageImmunity.grantImmunity(player, world.getTime());
        }
        
        world.playSound(null, player.getX(), player.getY(), player.getZ(), 
            SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        
        return true;
    }
    
    private static Vec3d findValidAirPosition(World world, Vec3d start, Vec3d direction, double maxDistance) {
        // Step through the path to find the furthest valid position
        double stepSize = 0.5;
        int steps = (int) (maxDistance / stepSize);
        Vec3d lastValidPosition = null;
        
        for (int i = 1; i <= steps; i++) {
            double distance = i * stepSize;
            Vec3d checkPos = start.add(direction.multiply(distance));
            
            BlockPos feetPos = new BlockPos((int) Math.floor(checkPos.x), (int) Math.floor(checkPos.y), (int) Math.floor(checkPos.z));
            BlockPos headPos = feetPos.up();
            
            BlockState feetBlock = world.getBlockState(feetPos);
            BlockState headBlock = world.getBlockState(headPos);
            
            if (isPassableBlock(feetBlock) && isPassableBlock(headBlock)) {
                lastValidPosition = checkPos;
            } else {
                // Hit obstacle, stop
                break;
            }
        }
        
        return lastValidPosition;
    }
    
    private static Vec3d getSurfacePosition(World world, BlockPos blockPos, net.minecraft.util.math.Direction side) {
        // Try to find a valid position on the block surface based on the side hit
        BlockPos candidatePos = blockPos.offset(side);
        
        // Check if player can stand here (2 blocks tall)
        BlockState feetBlock = world.getBlockState(candidatePos);
        BlockState headBlock = world.getBlockState(candidatePos.up());
        
        if (isPassableBlock(feetBlock) && isPassableBlock(headBlock)) {
            // Valid position on surface
            return new Vec3d(candidatePos.getX() + 0.5, candidatePos.getY(), candidatePos.getZ() + 0.5);
        }
        
        // Try to find a valid position above the block
        for (int i = 1; i <= 3; i++) {
            BlockPos abovePos = blockPos.up(i);
            BlockState aboveFeet = world.getBlockState(abovePos);
            BlockState aboveHead = world.getBlockState(abovePos.up());
            
            if (isPassableBlock(aboveFeet) && isPassableBlock(aboveHead)) {
                return new Vec3d(abovePos.getX() + 0.5, abovePos.getY(), abovePos.getZ() + 0.5);
            }
        }
        
        return null;
    }
    
    private static boolean isPassableBlock(BlockState state) {
        if (state.isAir()) {
            return true;
        }
        
        if (state.isLiquid()) {
            return true;
        }
        
        if (!state.blocksMovement()) {
            return true;
        }
        
        return false;
    }

    private static void createExplosion(PlayerEntity player, World world, int level) {
        // Increased explosion power
        float power = 5.0f; // Level 1: 5.0 (was 4.0)
        if (level >= 2) power = 6.0f; // Level 2: 6.0
        if (level >= 3) power = 7.0f; // Level 3: 7.0
        
        boolean breakBlocks = (level >= 3);
        
        if (!(world instanceof ServerWorld serverWorld)) return;

        // Visuals and Sound
        serverWorld.spawnParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY(), player.getZ(), 1, 0, 0, 0, 0);
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0f, 1.0f);

        // Damage Entities
        double radius = 6.0;
        List<Entity> entities = world.getOtherEntities(player, player.getBoundingBox().expand(radius));
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity) {
                float damage = 10.0f;
                if (level >= 2) damage += 5.0f;
                
                entity.damage(serverWorld, player.getDamageSources().explosion(null), damage);
            }
        }

        // Break Blocks (Level 3 only)
        if (breakBlocks) {
            world.createExplosion(player, player.getX(), player.getY(), player.getZ(), power, World.ExplosionSourceType.MOB);
        }
    }
    
    private static void reduceDurability(PlayerEntity player, int level) {
        ItemStack mainHandStack = player.getStackInHand(Hand.MAIN_HAND);
        
        if (mainHandStack.isEmpty() || !mainHandStack.isDamageable()) {
            return;
        }
        
        // Calculate chance to reduce durability based on level
        double reduceChance;
        if (level == 1) {
            reduceChance = 1.0; // 100% chance
        } else if (level == 2) {
            reduceChance = 0.5; // 50% chance
        } else { // level == 3
            reduceChance = 1.0 / 3.0; // 33% chance
        }
        
        // Random check for durability reduction
        if (Math.random() < reduceChance) {
            mainHandStack.damage(1, player, player.getPreferredEquipmentSlot(mainHandStack));
        }
    }

    private static void applyAbsorption(PlayerEntity player, int level) {
        // Prevent stacking: only apply if player doesn't already have absorption
        if (player.hasStatusEffect(StatusEffects.ABSORPTION)) {
            return;
        }

        int duration = 20 * 10; // 10 seconds
        int amplifier = 0;
        if (level >= 2) amplifier = 1; // Absorption II
        
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, duration, amplifier, false, false));
    }
}
