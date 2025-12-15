package com.leo.enchants.client;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.item.ItemStack;

/**
 * Render state for Shadow Clone entities.
 * Stores data needed for rendering the player-like shadow appearance.
 */
public class ShadowCloneRenderState extends LivingEntityRenderState {
    public float bodyYaw = 0.0f;
    public float headYaw = 0.0f;
    public float limbSwing = 0.0f;
    public float limbSwingAmount = 1.0f;
    public ItemStack weaponStack = ItemStack.EMPTY;
    public int ageTicks = 0;
    public boolean isSlim = false;
    public float tickDelta = 0.0f;  // For smooth interpolation
    public float yawDegrees = 0.0f;
    public float entityPitch = 0.0f;
}

