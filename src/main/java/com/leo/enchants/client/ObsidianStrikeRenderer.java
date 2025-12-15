package com.leo.enchants.client;

import com.leo.enchants.entity.ObsidianStrikeEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Renderer for ObsidianStrikeEntity.
 * The strike uses actual placed blocks for visuals, so this renderer is minimal.
 * It primarily exists to satisfy the entity renderer registration requirement.
 */
public class ObsidianStrikeRenderer extends EntityRenderer<ObsidianStrikeEntity, ObsidianStrikeRenderState> {
    
    public ObsidianStrikeRenderer(EntityRendererFactory.Context context) {
        super(context);
    }
    
    @Override
    public ObsidianStrikeRenderState createRenderState() {
        return new ObsidianStrikeRenderState();
    }
    
    @Override
    public void updateRenderState(ObsidianStrikeEntity entity, ObsidianStrikeRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        // The entity manages its own block placement, no custom rendering needed
    }
    
    @Override
    public void render(ObsidianStrikeRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        // No custom rendering - the entity uses placed blocks for visuals
    }
}

