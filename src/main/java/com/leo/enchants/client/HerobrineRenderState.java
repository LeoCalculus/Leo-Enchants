package com.leo.enchants.client;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;

/**
 * Render state for Herobrine entity.
 * Stores data needed for rendering the boss.
 */
public class HerobrineRenderState extends LivingEntityRenderState {
    public float bodyYaw = 0.0f;
    public float headYaw = 0.0f;
    public int phase = 1;
    public boolean isAttacking = false;
    public int attackType = 0;
    public int attackAnimationTicks = 0;
    public int swordSwingTicks = 0;
    public float tickDelta = 0.0f;
    public boolean isSkyAttack = false;
    public boolean isDying = false;
    public int deathAnimationTicks = 0;
}
