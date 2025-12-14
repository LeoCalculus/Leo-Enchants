package com.leo.enchants.client;

import net.minecraft.client.render.entity.state.EntityRenderState;

public class GiantSwordRenderState extends EntityRenderState {
    public float swordSize = 50.0f;
    public int level = 1;
    public boolean hasLanded = false;
    public float ticksExisted = 0;
    public String swordType = "minecraft:diamond_sword";
}
