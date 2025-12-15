package com.leo.enchants.logic;

import com.leo.enchants.LeoEnchantsMod;
import com.leo.enchants.entity.ShadowCloneEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles the Shadow Assassin chestplate enchantment.
 * When triggered, spawns three shadow clones that strike the targeted entity.
 * 
 * If clones are destroyed before attacking, the player loses part of their
 * health restoration - each destroyed clone means 1/3 of health won't be restored.
 */
public class ShadowAssassinHandler {

    private static final Identifier SHADOW_ASSASSIN_ID = Identifier.of(LeoEnchantsMod.MOD_ID, "shadow_assassin");
    private static final double TARGET_RANGE = 20.0;
    private static final int TRAVEL_TICKS = 10;        // Faster approach
    private static final int COOLDOWN_TICKS = 0;       // No cooldown
    private static final int TOTAL_CLONES = 3;

    /**
     * Tracks pending health restoration with destroyed clone count.
     */
    private record PendingRestore(float totalAmount, long restoreTick, AtomicInteger destroyedClones) {}

    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final Map<UUID, PendingRestore> pendingRestores = new HashMap<>();
    
    /**
     * Tracks active shadow clones per player. Max 3 at a time.
     */
    private static final Map<UUID, List<ShadowCloneEntity>> activeClones = new HashMap<>();
    private static final int MAX_ACTIVE_CLONES = 3;

    /**
     * Attempts to activate the enchantment when the player uses an item while aiming at an entity.
     */
    public static ActionResult tryActivate(PlayerEntity player, World world, Hand hand) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.PASS;
        }
        if (world.isClient) {
            return ActionResult.PASS;
        }

        // Must have the enchantment on chest armor
        if (!hasShadowAssassin(serverPlayer.getEquippedStack(EquipmentSlot.CHEST))) {
            return ActionResult.PASS;
        }

        long currentTime = world.getTime();
        if (currentTime < cooldowns.getOrDefault(serverPlayer.getUuid(), 0L)) {
            return ActionResult.PASS;
        }
        
        // Check if player already has max active clones
        int currentActiveClones = countActiveClones(serverPlayer.getUuid());
        if (currentActiveClones >= MAX_ACTIVE_CLONES) {
            return ActionResult.PASS;
        }

        LivingEntity target = findTarget(serverPlayer);
        if (target == null) {
            return ActionResult.PASS;
        }

        float healthBefore = serverPlayer.getHealth();
        float cost = Math.min(healthBefore * 0.5f, healthBefore - 1.0f);
        if (cost <= 0.0f) {
            return ActionResult.PASS;
        }

        serverPlayer.setHealth(healthBefore - cost);

        // Always use main hand weapon for shadow clone attacks
        ItemStack weapon = serverPlayer.getMainHandStack().copy();
        float damage = computeDamage(serverPlayer, weapon, target);

        // Create the pending restore tracker with destroyed clone counter
        AtomicInteger destroyedCounter = new AtomicInteger(0);
        PendingRestore restore = new PendingRestore(cost, currentTime + TRAVEL_TICKS + 2, destroyedCounter);
        pendingRestores.put(serverPlayer.getUuid(), restore);

        spawnClones(serverPlayer, target, weapon, damage, destroyedCounter);

        cooldowns.put(serverPlayer.getUuid(), currentTime + COOLDOWN_TICKS);

        serverPlayer.getWorld().playSound(
            null,
            serverPlayer.getX(),
            serverPlayer.getY(),
            serverPlayer.getZ(),
            net.minecraft.sound.SoundEvents.ENTITY_ENDERMAN_SCREAM,
            net.minecraft.sound.SoundCategory.PLAYERS,
            0.9f,
            0.5f
        );

        return ActionResult.SUCCESS;
    }

    /**
     * Tick restoration timers and refund health based on surviving clones.
     * Each destroyed clone reduces restoration by 1/3.
     */
    public static void tickRestorations(net.minecraft.server.MinecraftServer server, long currentTime) {
        Iterator<Map.Entry<UUID, PendingRestore>> iterator = pendingRestores.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingRestore> entry = iterator.next();
            PendingRestore restore = entry.getValue();

            if (currentTime >= restore.restoreTick()) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
                if (player != null && player.isAlive()) {
                    // Calculate how much to restore based on surviving clones
                    int destroyed = restore.destroyedClones().get();
                    int surviving = TOTAL_CLONES - destroyed;
                    
                    // Each surviving clone restores 1/3 of the total cost
                    float restoreAmount = restore.totalAmount() * surviving / TOTAL_CLONES;
                    
                    if (restoreAmount > 0) {
                        float newHealth = Math.min(player.getMaxHealth(), player.getHealth() + restoreAmount);
                        player.setHealth(newHealth);
                    }
                    
                    // Notify player if they lost health due to destroyed clones
                    if (destroyed > 0) {
                        float lostHealth = restore.totalAmount() * destroyed / TOTAL_CLONES;
                        LeoEnchantsMod.LOGGER.debug("Shadow clone(s) destroyed - {} health not restored for player {}", 
                            lostHealth, player.getName().getString());
                    }
                }
                iterator.remove();
            }
        }
    }

    public static void clearPlayer(UUID playerId) {
        cooldowns.remove(playerId);
        pendingRestores.remove(playerId);
        activeClones.remove(playerId);
    }
    
    /**
     * Counts active (alive) clones for a player, cleaning up dead ones.
     */
    private static int countActiveClones(UUID playerId) {
        List<ShadowCloneEntity> clones = activeClones.get(playerId);
        if (clones == null) {
            return 0;
        }
        
        // Remove dead/discarded clones
        clones.removeIf(clone -> clone == null || !clone.isAlive() || clone.isRemoved());
        
        return clones.size();
    }
    
    /**
     * Registers a new clone as active for a player.
     */
    private static void registerClone(UUID playerId, ShadowCloneEntity clone) {
        activeClones.computeIfAbsent(playerId, k -> new ArrayList<>()).add(clone);
    }

    private static boolean hasShadowAssassin(ItemStack chest) {
        if (chest.isEmpty()) {
            return false;
        }
        ItemEnchantmentsComponent enchants = chest.get(DataComponentTypes.ENCHANTMENTS);
        if (enchants == null) {
            return false;
        }

        for (var entry : enchants.getEnchantmentEntries()) {
            if (entry.getKey().matchesId(SHADOW_ASSASSIN_ID) && entry.getIntValue() > 0) {
                return true;
            }
        }
        return false;
    }

    private static LivingEntity findTarget(ServerPlayerEntity player) {
        Vec3d start = player.getCameraPosVec(1.0f);
        Vec3d look = player.getRotationVec(1.0f);
        Vec3d end = start.add(look.multiply(TARGET_RANGE));

        Box searchBox = player.getBoundingBox().stretch(look.multiply(TARGET_RANGE)).expand(1.0);
        EntityHitResult hit = ProjectileUtil.raycast(
            player,
            start,
            end,
            searchBox,
            entity -> entity instanceof LivingEntity living
                && entity.isAlive()
                && entity != player
                && !entity.isSpectator()
                && living.isAttackable()
                && !(entity instanceof com.leo.enchants.entity.ShadowCloneEntity),
            TARGET_RANGE * TARGET_RANGE
        );

        if (hit != null && hit.getEntity() instanceof LivingEntity living) {
            return living;
        }

        // Fallback: pick the closest living entity in front cone if raycast missed
        LivingEntity best = null;
        double bestDot = 0.6; // require roughly within ~53 degrees
        double bestDistSq = TARGET_RANGE * TARGET_RANGE;
        for (var entity : player.getWorld().getOtherEntities(player, searchBox, e ->
            e instanceof LivingEntity living
                && e.isAlive()
                && e != player
                && !e.isSpectator()
                && living.isAttackable()
                && !(e instanceof com.leo.enchants.entity.ShadowCloneEntity))) {
            LivingEntity living = (LivingEntity) entity;
            Vec3d to = living.getPos().subtract(start);
            double distSq = to.lengthSquared();
            if (distSq > bestDistSq) continue;
            Vec3d dir = to.normalize();
            double dot = dir.dotProduct(look.normalize());
            if (dot > bestDot) {
                bestDot = dot;
                bestDistSq = distSq;
                best = living;
            }
        }
        return best;
    }

    private static float computeDamage(ServerPlayerEntity player, ItemStack weapon, LivingEntity target) {
        double base = player.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
        return (float) base;
    }

    private static void spawnClones(ServerPlayerEntity player, LivingEntity target, ItemStack weapon, 
                                    float damage, AtomicInteger destroyedCounter) {
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        // Callback when a clone is destroyed - increment the counter
        java.util.function.Consumer<ShadowCloneEntity> onDestroyed = (clone) -> {
            destroyedCounter.incrementAndGet();
            
            // Play a sound when clone is destroyed
            serverWorld.playSound(
                null,
                clone.getX(),
                clone.getY(),
                clone.getZ(),
                net.minecraft.sound.SoundEvents.ENTITY_PHANTOM_DEATH,
                net.minecraft.sound.SoundCategory.PLAYERS,
                0.7f,
                1.5f
            );
        };

        ShadowCloneEntity center = ShadowCloneEntity.create(serverWorld, player, target, weapon, 0.0, TRAVEL_TICKS, damage, onDestroyed);
        ShadowCloneEntity left = ShadowCloneEntity.create(serverWorld, player, target, weapon, 60.0, TRAVEL_TICKS, damage, onDestroyed);
        ShadowCloneEntity right = ShadowCloneEntity.create(serverWorld, player, target, weapon, -60.0, TRAVEL_TICKS, damage, onDestroyed);

        serverWorld.spawnEntity(center);
        serverWorld.spawnEntity(left);
        serverWorld.spawnEntity(right);
        
        // Register all clones as active
        UUID playerId = player.getUuid();
        registerClone(playerId, center);
        registerClone(playerId, left);
        registerClone(playerId, right);
    }
}
