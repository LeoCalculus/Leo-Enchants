package com.leo.enchants.logic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class StrafeHandler {
    private static final Identifier STRAFE_ID = Identifier.of("leo_enchants", "strafe");
    
    // Air control speed - how fast player can change direction in air
    private static final double AIR_CONTROL_SPEED = 0.15;
    // Maximum horizontal speed
    private static final double MAX_HORIZONTAL_SPEED = 0.5;
    
    /**
     * Called every client tick to handle strafe/air control logic
     */
    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        
        // Only apply when in the air
        if (player.isOnGround()) return;
        
        // Check if player has strafe enchantment on boots
        if (!hasStrafeEnchantment(player)) return;
        
        // Get movement input
        GameOptions options = client.options;
        boolean forward = options.forwardKey.isPressed();
        boolean backward = options.backKey.isPressed();
        boolean left = options.leftKey.isPressed();
        boolean right = options.rightKey.isPressed();
        
        // If no movement keys pressed, don't modify velocity
        if (!forward && !backward && !left && !right) return;
        
        // Calculate desired movement direction based on player's look direction and input
        float yaw = player.getYaw();
        double yawRad = Math.toRadians(yaw);
        
        // Calculate forward and right vectors based on player's yaw
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);
        
        // Build movement vector from input
        double moveX = 0;
        double moveZ = 0;
        
        if (forward) {
            moveX += forwardX;
            moveZ += forwardZ;
        }
        if (backward) {
            moveX -= forwardX;
            moveZ -= forwardZ;
        }
        if (left) {
            moveX += rightX;
            moveZ += rightZ;
        }
        if (right) {
            moveX -= rightX;
            moveZ -= rightZ;
        }
        
        // Normalize movement vector
        double length = Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (length > 0) {
            moveX /= length;
            moveZ /= length;
        }
        
        // Get current velocity
        Vec3d currentVelocity = player.getVelocity();
        
        // Calculate new horizontal velocity by steering towards desired direction
        double newVelX = currentVelocity.x + moveX * AIR_CONTROL_SPEED;
        double newVelZ = currentVelocity.z + moveZ * AIR_CONTROL_SPEED;
        
        // Clamp horizontal speed to prevent infinite acceleration
        double horizontalSpeed = Math.sqrt(newVelX * newVelX + newVelZ * newVelZ);
        if (horizontalSpeed > MAX_HORIZONTAL_SPEED) {
            double scale = MAX_HORIZONTAL_SPEED / horizontalSpeed;
            newVelX *= scale;
            newVelZ *= scale;
        }
        
        // Apply new velocity (keep vertical velocity unchanged)
        player.setVelocity(newVelX, currentVelocity.y, newVelZ);
    }
    
    /**
     * Check if player's boots have the strafe enchantment
     */
    private static boolean hasStrafeEnchantment(ClientPlayerEntity player) {
        ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);
        
        if (boots.isEmpty()) {
            return false;
        }
        
        ItemEnchantmentsComponent enchantments = boots.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null) {
            return false;
        }
        
        for (var entry : enchantments.getEnchantmentEntries()) {
            if (entry.getKey().matchesId(STRAFE_ID)) {
                return true;
            }
        }
        
        return false;
    }
}

