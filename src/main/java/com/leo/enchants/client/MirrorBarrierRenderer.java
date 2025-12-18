package com.leo.enchants.client;

import com.leo.enchants.entity.MirrorBarrierEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.math.RotationAxis;

/**
 * Renderer for MirrorBarrierEntity.
 * Renders a floating, rotating item with visual effects.
 */
public class MirrorBarrierRenderer extends EntityRenderer<MirrorBarrierEntity, MirrorBarrierRenderState> {
    
    private final ItemRenderer itemRenderer;
    
    public MirrorBarrierRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = MinecraftClient.getInstance().getItemRenderer();
    }
    
    @Override
    public MirrorBarrierRenderState createRenderState() {
        return new MirrorBarrierRenderState();
    }
    
    @Override
    public void updateRenderState(MirrorBarrierEntity entity, MirrorBarrierRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.displayedItem = entity.getDisplayedItem();
        state.rotationTicks = entity.getRotationTicks();
        state.floatOffset = entity.getFloatOffset();
        state.tickDelta = tickDelta;
    }
    
    @Override
    public void render(MirrorBarrierRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (state.displayedItem.isEmpty()) {
            return;
        }
        
        matrices.push();
        
        // Apply floating offset
        matrices.translate(0, 0.5 + state.floatOffset, 0);
        
        // Rotate the item smoothly
        float rotation = (state.rotationTicks + state.tickDelta) * 2.0f;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
        
        // Scale up the item slightly
        matrices.scale(1.5f, 1.5f, 1.5f);
        
        // Render the item using the same pattern as GiantSwordEntityRenderer
        itemRenderer.renderItem(
            state.displayedItem,
            ItemDisplayContext.GROUND,
            light,
            OverlayTexture.DEFAULT_UV,
            matrices,
            vertexConsumers,
            null,
            0
        );
        
        matrices.pop();
    }
}
