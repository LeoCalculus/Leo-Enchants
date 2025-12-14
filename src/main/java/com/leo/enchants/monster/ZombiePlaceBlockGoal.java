package com.leo.enchants.monster;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.List;

/**
 * AI Goal that allows zombies to place blocks when they can't reach their target.
 * Zombies will bridge gaps, build up to reach higher players, and cross lava/void.
 */
public class ZombiePlaceBlockGoal extends Goal {
    
    private final ZombieEntity zombie;
    
    // Overworld-only blocks that zombies can use
    private static final List<Block> OVERWORLD_BLOCKS = List.of(
        Blocks.DIRT,
        Blocks.COBBLESTONE,
        Blocks.STONE,
        Blocks.GRAVEL,
        Blocks.OAK_PLANKS
    );
    
    // The block type this zombie uses (chosen once, stays consistent)
    private Block chosenBlock = null;
    
    // Cooldown between placing blocks
    private static final int PLACE_COOLDOWN = 10;
    private int cooldownTimer = 0;
    
    // Track last known reachable state to avoid flickering
    private int unreachableTicks = 0;
    private static final int UNREACHABLE_THRESHOLD = 10; // Must be unreachable for 0.5 seconds
    
    public ZombiePlaceBlockGoal(ZombieEntity zombie) {
        this.zombie = zombie;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }
    
    @Override
    public boolean canStart() {
        LivingEntity target = zombie.getTarget();
        if (target == null || !target.isAlive()) {
            unreachableTicks = 0;
            return false;
        }
        
        // Check if zombie can actually reach the target
        if (canReachTarget(target)) {
            unreachableTicks = 0;
            return false;
        }
        
        // Count how long we've been unable to reach
        unreachableTicks++;
        
        // Only start if we've been unable to reach for a short while
        // This prevents building when zombie is just walking toward player
        return unreachableTicks >= UNREACHABLE_THRESHOLD;
    }
    
    /**
     * Check if zombie can reach the target through normal pathfinding
     */
    private boolean canReachTarget(LivingEntity target) {
        // First check: is the target close enough to attack already?
        double distanceSq = zombie.squaredDistanceTo(target);
        if (distanceSq < 4.0) { // Within 2 blocks
            return true;
        }
        
        // Second check: can we pathfind to within attack range?
        Path path = zombie.getNavigation().findPathTo(target, 1);
        if (path != null && path.reachesTarget()) {
            return true;
        }
        
        // Third check: is the zombie already navigating toward target successfully?
        Path currentPath = zombie.getNavigation().getCurrentPath();
        if (currentPath != null && !currentPath.isFinished()) {
            // Check if path end is close to target
            if (currentPath.getLength() > 0) {
                var endNode = currentPath.getEnd();
                if (endNode != null) {
                    double pathEndDist = target.squaredDistanceTo(endNode.x, endNode.y, endNode.z);
                    if (pathEndDist < 9.0) { // Path ends within 3 blocks of target
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    @Override
    public boolean shouldContinue() {
        LivingEntity target = zombie.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        
        // Stop if we can now reach the target
        if (canReachTarget(target)) {
            unreachableTicks = 0;
            return false;
        }
        
        return true;
    }
    
    @Override
    public void start() {
        cooldownTimer = 0;
        
        // Choose a consistent block type for this zombie
        if (chosenBlock == null) {
            chosenBlock = OVERWORLD_BLOCKS.get(zombie.getWorld().random.nextInt(OVERWORLD_BLOCKS.size()));
        }
    }
    
    @Override
    public void tick() {
        LivingEntity target = zombie.getTarget();
        if (target == null) return;
        
        // Cooldown check
        if (cooldownTimer > 0) {
            cooldownTimer--;
            return;
        }
        
        // Look at target
        zombie.getLookControl().lookAt(target, 30.0F, 30.0F);
        
        BlockPos zombiePos = zombie.getBlockPos();
        BlockPos targetPos = target.getBlockPos();
        
        // Priority 1: Need to go UP (target is higher)
        if (targetPos.getY() > zombiePos.getY()) {
            if (tryBuildUp(target)) {
                return;
            }
        }
        
        // Priority 2: Need to bridge a gap
        if (tryBridge(target)) {
            return;
        }
        
        // Priority 3: General path assistance
        tryGeneralBuild(target);
    }
    
    /**
     * Build upward to reach a higher target.
     * Places block adjacent to zombie, then zombie walks onto it.
     */
    private boolean tryBuildUp(LivingEntity target) {
        BlockPos zombiePos = zombie.getBlockPos();
        World world = zombie.getWorld();
        
        Vec3d direction = target.getPos().subtract(zombie.getPos()).normalize();
        Direction primaryDir = Direction.getFacing(direction.x, 0, direction.z);
        
        // Strategy: Place a block at zombie's level in the direction of target
        // This creates a step the zombie can walk up onto
        
        // Try primary direction first
        BlockPos stepPos = zombiePos.offset(primaryDir);
        if (tryPlaceStep(stepPos)) {
            return true;
        }
        
        // Try other horizontal directions
        for (Direction dir : Direction.Type.HORIZONTAL) {
            if (dir == primaryDir) continue;
            
            stepPos = zombiePos.offset(dir);
            if (tryPlaceStep(stepPos)) {
                return true;
            }
        }
        
        // If all else fails, try placing directly adjacent at same level
        // to create a pillar the zombie can climb by placing blocks beside it
        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos adjacentUp = zombiePos.offset(dir).up();
            BlockPos adjacentBase = adjacentUp.down();
            
            // Place a raised block that zombie can jump to
            if (world.getBlockState(adjacentBase).isSolidBlock(world, adjacentBase) &&
                canPlaceBlockAt(adjacentUp) && 
                hasAdjacentSolid(adjacentUp) &&
                world.getBlockState(adjacentUp.up()).isAir()) {
                
                placeBlock(adjacentUp);
                cooldownTimer = PLACE_COOLDOWN;
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Try to place a step block at the given position
     */
    private boolean tryPlaceStep(BlockPos stepPos) {
        World world = zombie.getWorld();
        BlockPos below = stepPos.down();
        
        // Check if we can place here and zombie can use it
        if (canPlaceBlockAt(stepPos) && 
            hasAdjacentSolid(stepPos) &&
            world.getBlockState(stepPos.up()).isAir() &&
            world.getBlockState(stepPos.up(2)).isAir()) {
            
            placeBlock(stepPos);
            cooldownTimer = PLACE_COOLDOWN;
            
            // Move toward the new step
            zombie.getNavigation().startMovingTo(
                stepPos.getX() + 0.5, 
                stepPos.getY() + 1, 
                stepPos.getZ() + 0.5, 
                1.0
            );
            return true;
        }
        
        return false;
    }
    
    /**
     * Bridge across gaps toward target
     */
    private boolean tryBridge(LivingEntity target) {
        BlockPos zombiePos = zombie.getBlockPos();
        World world = zombie.getWorld();
        
        Vec3d direction = target.getPos().subtract(zombie.getPos()).normalize();
        Direction primaryDir = Direction.getFacing(direction.x, 0, direction.z);
        
        // Look for gaps in front of zombie that need bridging
        for (int distance = 1; distance <= 4; distance++) {
            BlockPos checkPos = zombiePos.offset(primaryDir, distance);
            BlockPos floorPos = checkPos.down();
            
            // Check if this is a gap that needs bridging
            if (isGapOrDanger(floorPos)) {
                // Find first valid place position
                for (int d = 1; d <= distance; d++) {
                    BlockPos bridgePos = zombiePos.offset(primaryDir, d).down();
                    
                    if (canPlaceBlockAt(bridgePos) && hasAdjacentSolid(bridgePos)) {
                        placeBlock(bridgePos);
                        cooldownTimer = PLACE_COOLDOWN;
                        return true;
                    }
                }
            }
        }
        
        // Check other directions too
        for (Direction dir : Direction.Type.HORIZONTAL) {
            if (dir == primaryDir) continue;
            
            BlockPos checkPos = zombiePos.offset(dir);
            BlockPos floorPos = checkPos.down();
            
            if (isGapOrDanger(floorPos) && canPlaceBlockAt(floorPos) && hasAdjacentSolid(floorPos)) {
                placeBlock(floorPos);
                cooldownTimer = PLACE_COOLDOWN;
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * General building to help reach target
     */
    private boolean tryGeneralBuild(LivingEntity target) {
        BlockPos zombiePos = zombie.getBlockPos();
        World world = zombie.getWorld();
        
        Vec3d direction = target.getPos().subtract(zombie.getPos()).normalize();
        Direction primaryDir = Direction.getFacing(direction.x, 0, direction.z);
        
        // Try placing floor in direction of target
        BlockPos frontPos = zombiePos.offset(primaryDir);
        BlockPos frontFloor = frontPos.down();
        
        if (canPlaceBlockAt(frontFloor) && hasAdjacentSolid(frontFloor)) {
            placeBlock(frontFloor);
            cooldownTimer = PLACE_COOLDOWN;
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if a position is a dangerous gap (lava, deep void, water)
     */
    private boolean isGapOrDanger(BlockPos pos) {
        World world = zombie.getWorld();
        BlockState state = world.getBlockState(pos);
        
        // Lava is always dangerous
        if (state.isOf(Blocks.LAVA)) {
            return true;
        }
        
        // Water blocks path
        if (state.isOf(Blocks.WATER)) {
            return true;
        }
        
        // Check for void/deep gap
        if (state.isAir() || state.isReplaceable()) {
            // Check how deep the gap is
            int depth = 0;
            for (int i = 1; i <= 5; i++) {
                BlockState below = world.getBlockState(pos.down(i));
                if (below.isSolidBlock(world, pos.down(i))) {
                    break;
                }
                if (below.isOf(Blocks.LAVA)) {
                    return true; // Lava below
                }
                depth++;
            }
            // Gap of 3+ blocks is dangerous
            return depth >= 3;
        }
        
        return false;
    }
    
    /**
     * Check if we can place a block at the given position
     */
    private boolean canPlaceBlockAt(BlockPos pos) {
        World world = zombie.getWorld();
        BlockState currentState = world.getBlockState(pos);
        
        return currentState.isAir() || 
               currentState.isOf(Blocks.WATER) || 
               currentState.isOf(Blocks.LAVA) ||
               currentState.isReplaceable();
    }
    
    /**
     * Check if there's an adjacent solid block to place against
     */
    private boolean hasAdjacentSolid(BlockPos pos) {
        World world = zombie.getWorld();
        
        for (Direction dir : Direction.values()) {
            BlockPos adjacent = pos.offset(dir);
            if (world.getBlockState(adjacent).isSolidBlock(world, adjacent)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Place the chosen block type
     */
    private void placeBlock(BlockPos pos) {
        World world = zombie.getWorld();
        
        if (!world.isClient() && chosenBlock != null) {
            world.setBlockState(pos, chosenBlock.getDefaultState());
            world.playSound(null, pos, SoundEvents.BLOCK_GRAVEL_PLACE, SoundCategory.HOSTILE, 1.0F, 0.8F + world.random.nextFloat() * 0.4F);
            zombie.swingHand(zombie.getActiveHand());
        }
    }
    
    @Override
    public void stop() {
        // Don't reset unreachableTicks here - let canStart handle it
    }
}
