package com.leo.enchants.client;

import com.leo.enchants.entity.ObsidianBridgeEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Renderer for ObsidianBridgeEntity.
 * The bridge uses actual placed blocks for visuals, so this renderer is minimal.
 * It primarily exists to satisfy the entity renderer registration requirement.
 */
public class ObsidianBridgeRenderer extends EntityRenderer<ObsidianBridgeEntity, ObsidianBridgeRenderState> {
    
    public ObsidianBridgeRenderer(EntityRendererFactory.Context context) {
        super(context);
    }
    
    @Override
    public ObsidianBridgeRenderState createRenderState() {
        return new ObsidianBridgeRenderState();
    }
    
    @Override
    public void updateRenderState(ObsidianBridgeEntity entity, ObsidianBridgeRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        // The entity manages its own block placement, no custom rendering needed
    }
    
    @Override
    public void render(ObsidianBridgeRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        // No custom rendering - the entity uses placed blocks for visuals
    }
}

