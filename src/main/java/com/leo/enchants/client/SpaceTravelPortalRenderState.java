package com.leo.enchants.client;

import net.minecraft.client.render.entity.state.EntityRenderState;

/**
 * Render state for SpaceTravelPortalEntity.
 * Contains all the data needed for rendering the curved portal surface.
 */
public class SpaceTravelPortalRenderState extends EntityRenderState {
    public String targetDimension = "minecraft:overworld";
    public float portalYaw = 0;
    public float portalPitch = 0;
    public int closingTicks = 0;
    public boolean isClosing = false;
    public int lifetimeTicks = 0;
    public float tickDelta = 0;
}

