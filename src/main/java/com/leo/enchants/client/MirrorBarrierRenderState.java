package com.leo.enchants.client;

import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.item.ItemStack;

/**
 * Render state for the MirrorBarrierEntity.
 */
public class MirrorBarrierRenderState extends EntityRenderState {
    public ItemStack displayedItem = ItemStack.EMPTY;
    public int rotationTicks = 0;
    public float floatOffset = 0;
    public float tickDelta = 0;
}
