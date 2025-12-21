package com.leo.enchants.logic;

import com.leo.enchants.LeoEnchantsMod;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the Quantum Tunnelling chestplate enchantment.
 * When wearing a chestplate with this enchantment and colliding with a wall,
 * the player will tunnel through to the other side if there's empty space
 * 50 blocks away in the direction of movement.
 */
public class QuantumTunnellingHandler {

    private static final Identifier QUANTUM_TUNNELLING_ID = Identifier.of(LeoEnchantsMod.MOD_ID, "quantum_tunnelling");
    private static final int TUNNEL_DISTANCE = 50;
    private static final int COOLDOWN_TICKS = 40; // 2 second cooldown

    // Track cooldowns per player
    private static final Map<UUID, Long> cooldowns = new HashMap<>();

    /**
     * Called every server tick for each player to check for quantum tunnelling
     */
    public static void tick(ServerPlayerEntity player, long currentTime) {
        UUID playerId = player.getUuid();

        // Check if player has the enchantment
        if (!hasQuantumTunnelling(player)) {
            return;
        }

        // Check if on cooldown
        if (currentTime < cooldowns.getOrDefault(playerId, 0L)) {
            return;
        }

        // Check if player is trying to move (has input velocity)
        Vec3d velocity = player.getVelocity();
        boolean isMoving = Math.abs(velocity.x) > 0.001 || Math.abs(velocity.z) > 0.001;
        
        // Also check if player is sprinting or has forward momentum
        boolean isTryingToMove = isMoving || player.isSprinting() || player.forwardSpeed > 0;
        
        if (!isTryingToMove) {
            return;
        }

        // Get the direction player is facing
        Direction moveDirection = player.getHorizontalFacing();

        // Use player's exact position to find blocks in front
        ServerWorld world = (ServerWorld) player.getWorld();
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        
        // Player width is 0.6, so check 0.35 blocks ahead from center (right at the edge of player hitbox)
        double checkDistance = 0.35;
        double checkX = px + moveDirection.getOffsetX() * checkDistance;
        double checkZ = pz + moveDirection.getOffsetZ() * checkDistance;
        
        // Get block positions at feet and head level in front of player
        BlockPos frontFeet = BlockPos.ofFloored(checkX, py + 0.1, checkZ); // Slightly above feet to avoid ground
        BlockPos frontHead = BlockPos.ofFloored(checkX, py + 1.5, checkZ); // At eye level

        BlockState frontFeetBlock = world.getBlockState(frontFeet);
        BlockState frontHeadBlock = world.getBlockState(frontHead);

        // Check if blocks have collision
        boolean blockedAtFeet = !frontFeetBlock.getCollisionShape(world, frontFeet).isEmpty();
        boolean blockedAtHead = !frontHeadBlock.getCollisionShape(world, frontHead).isEmpty();

        // Only trigger if there's a block with collision in front AND player is actually colliding
        // Use horizontalCollision OR check if we're very close to a blocking block
        boolean isAgainstWall = player.horizontalCollision || isPlayerAgainstBlock(player, moveDirection, world);
        
        if ((blockedAtFeet || blockedAtHead) && isAgainstWall) {
            attemptTunnel(player, currentTime, moveDirection);
        }
    }
    
    /**
     * Check if player is very close to a solid block in the given direction
     */
    private static boolean isPlayerAgainstBlock(ServerPlayerEntity player, Direction dir, ServerWorld world) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        
        // Check at a distance where player would be touching the block (player width = 0.6, so radius = 0.3)
        // Add small buffer (0.32) to detect when player is pressed against block
        double checkDist = 0.32;
        double checkX = px + dir.getOffsetX() * checkDist;
        double checkZ = pz + dir.getOffsetZ() * checkDist;
        
        BlockPos checkFeet = BlockPos.ofFloored(checkX, py + 0.1, checkZ);
        BlockPos checkHead = BlockPos.ofFloored(checkX, py + 1.5, checkZ);
        
        BlockState feetState = world.getBlockState(checkFeet);
        BlockState headState = world.getBlockState(checkHead);
        
        boolean feetBlocked = !feetState.getCollisionShape(world, checkFeet).isEmpty();
        boolean headBlocked = !headState.getCollisionShape(world, checkHead).isEmpty();
        
        return feetBlocked || headBlocked;
    }

    /**
     * Check if player's chestplate has the quantum tunnelling enchantment
     */
    private static boolean hasQuantumTunnelling(ServerPlayerEntity player) {
        ItemStack chestplate = player.getEquippedStack(EquipmentSlot.CHEST);

        if (chestplate.isEmpty()) {
            return false;
        }

        ItemEnchantmentsComponent enchantments = chestplate.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null) {
            return false;
        }

        for (var entry : enchantments.getEnchantmentEntries()) {
            if (entry.getKey().matchesId(QUANTUM_TUNNELLING_ID)) {
                return entry.getIntValue() > 0;
            }
        }

        return false;
    }

    /**
     * Attempt to tunnel through the wall
     */
    private static void attemptTunnel(ServerPlayerEntity player, long currentTime, Direction moveDirection) {
        BlockPos playerPos = player.getBlockPos();
        ServerWorld world = (ServerWorld) player.getWorld();
        
        // Scan through the wall to find the first safe position (up to TUNNEL_DISTANCE blocks)
        // Start from 1 block ahead (we're already colliding with position 0)
        BlockPos targetPos = null;
        boolean passedThroughSolid = false;
        
        for (int distance = 1; distance <= TUNNEL_DISTANCE; distance++) {
            BlockPos checkPos = playerPos.offset(moveDirection, distance);
            BlockPos headPos = checkPos.up();
            
            BlockState feetBlock = world.getBlockState(checkPos);
            BlockState headBlock = world.getBlockState(headPos);
            
            // Check collision shape - empty means passable
            boolean feetPassable = feetBlock.getCollisionShape(world, checkPos).isEmpty();
            boolean headPassable = headBlock.getCollisionShape(world, headPos).isEmpty();
            
            if (!feetPassable || !headPassable) {
                // We're inside the wall (has collision)
                passedThroughSolid = true;
            } else if (passedThroughSolid) {
                // We've passed through solid blocks and found air - this is our exit point!
                targetPos = checkPos;
                break;
            }
            // If we haven't hit any solid yet, keep scanning (player might not be right against the wall)
        }

        // Only tunnel if we found a valid exit point after passing through solid blocks
        if (targetPos != null) {
            performTunnel(player, targetPos, moveDirection, currentTime);
        }
    }

    /**
     * Get horizontal direction from velocity, or null if not moving horizontally
     */
    private static Direction getHorizontalFacingFromVelocity(ServerPlayerEntity player, Vec3d velocity) {
        double absX = Math.abs(velocity.x);
        double absZ = Math.abs(velocity.z);

        // Require some minimum horizontal velocity to determine direction
        double threshold = 0.01;
        if (absX < threshold && absZ < threshold) {
            return null;
        }

        if (absX > absZ) {
            return velocity.x > 0 ? Direction.EAST : Direction.WEST;
        } else {
            return velocity.z > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    /**
     * Check if the destination has enough space for the player (2 blocks tall, 1 wide)
     */
    private static boolean isSafeDestination(ServerPlayerEntity player, BlockPos targetPos) {
        ServerWorld world = (ServerWorld) player.getWorld();

        // Check the two blocks where the player would stand (feet and head)
        BlockPos feetPos = targetPos;
        BlockPos headPos = targetPos.up();

        BlockState feetBlock = world.getBlockState(feetPos);
        BlockState headBlock = world.getBlockState(headPos);

        // Check if both blocks are passable (not solid)
        return !feetBlock.isSolidBlock(world, feetPos) && !headBlock.isSolidBlock(world, headPos);
    }

    /**
     * Perform the quantum tunnel teleportation
     */
    private static void performTunnel(ServerPlayerEntity player, BlockPos targetPos, Direction direction, long currentTime) {
        ServerWorld world = (ServerWorld) player.getWorld();

        // Store original position for particles
        double origX = player.getX();
        double origY = player.getY();
        double origZ = player.getZ();

        // Calculate exact teleport position (center of block)
        double destX = targetPos.getX() + 0.5;
        double destY = targetPos.getY();
        double destZ = targetPos.getZ() + 0.5;

        // Spawn departure particles (quantum uncertainty effect)
        spawnTunnelParticles(world, origX, origY + 1, origZ);

        // Teleport the player
        player.teleport(world, destX, destY, destZ, java.util.Set.of(), player.getYaw(), player.getPitch(), false);

        // Reset velocity to prevent weird movement after teleport
        player.setVelocity(Vec3d.ZERO);
        player.velocityModified = true;

        // Spawn arrival particles
        spawnTunnelParticles(world, destX, destY + 1, destZ);

        // Play quantum tunnelling sound
        world.playSound(
            null,
            origX, origY, origZ,
            SoundEvents.ENTITY_ENDERMAN_TELEPORT,
            SoundCategory.PLAYERS,
            1.0f,
            1.5f
        );
        world.playSound(
            null,
            destX, destY, destZ,
            SoundEvents.ENTITY_ENDERMAN_TELEPORT,
            SoundCategory.PLAYERS,
            1.0f,
            1.5f
        );

        // Set cooldown
        cooldowns.put(player.getUuid(), currentTime + COOLDOWN_TICKS);

        LeoEnchantsMod.LOGGER.debug("Player {} quantum tunnelled {} blocks {}", 
            player.getName().getString(), TUNNEL_DISTANCE, direction);
    }

    /**
     * Spawn quantum-like particle effects
     */
    private static void spawnTunnelParticles(ServerWorld world, double x, double y, double z) {
        // Spawn portal-like particles in a spiral pattern
        for (int i = 0; i < 30; i++) {
            double angle = (i / 30.0) * Math.PI * 4; // Two full rotations
            double radius = 0.5 + (i / 30.0) * 0.3;
            double height = (i / 30.0) * 2.0;

            double px = x + Math.cos(angle) * radius;
            double py = y - 1 + height;
            double pz = z + Math.sin(angle) * radius;

            world.spawnParticles(
                ParticleTypes.REVERSE_PORTAL,
                px, py, pz,
                1,
                0.1, 0.1, 0.1,
                0.02
            );
        }

        // Add some enchant glyphs for that quantum magic feel
        for (int i = 0; i < 10; i++) {
            double offsetX = (Math.random() - 0.5) * 1.5;
            double offsetY = Math.random() * 2;
            double offsetZ = (Math.random() - 0.5) * 1.5;

            world.spawnParticles(
                ParticleTypes.ENCHANT,
                x + offsetX, y - 1 + offsetY, z + offsetZ,
                1,
                0, 0, 0,
                0.5
            );
        }
    }

    /**
     * Clean up player data (call when player disconnects)
     */
    public static void removePlayer(UUID playerId) {
        cooldowns.remove(playerId);
    }
}

