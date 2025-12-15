package com.leo.enchants.client;

import com.leo.enchants.entity.DigitDisintegrationEntity;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;

/**
 * Renders floating "1" and "0" digits for the de-enchant disintegration effect.
 * Uses text rendering for a clean, digital look.
 */
public class DigitDisintegrationRenderer extends EntityRenderer<DigitDisintegrationEntity, DigitDisintegrationRenderState> {
    
    private final TextRenderer textRenderer;
    
    // Glowing cyan/green color for the "Matrix" effect
    private static final int COLOR_ONE = 0x00FF41;  // Matrix green for "1"
    private static final int COLOR_ZERO = 0x00D9FF; // Cyan for "0"
    
    public DigitDisintegrationRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.textRenderer = context.getTextRenderer();
    }
    
    @Override
    public DigitDisintegrationRenderState createRenderState() {
        return new DigitDisintegrationRenderState();
    }
    
    @Override
    public void updateRenderState(DigitDisintegrationEntity entity, DigitDisintegrationRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.isOne = entity.isOne();
        state.alpha = entity.getAlpha();
        state.scale = entity.getScale();
        state.rotationYaw = entity.getYaw();
        state.ageTicks = entity.getAgeTicks();
    }
    
    @Override
    public void render(DigitDisintegrationRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        
        // Make the digit always face the camera (billboard effect)
        matrices.multiply(this.dispatcher.getRotation());
        
        // Apply rotation around Y axis for some variation
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(state.rotationYaw));
        
        // Scale the text
        float scale = state.scale * 0.05f; // Text is large by default, scale it down
        matrices.scale(-scale, -scale, scale);
        
        // Choose digit and color
        String digit = state.isOne ? "1" : "0";
        int baseColor = state.isOne ? COLOR_ONE : COLOR_ZERO;
        
        // Apply alpha to color
        int alpha = (int) (state.alpha * 255);
        int color = (alpha << 24) | (baseColor & 0x00FFFFFF);
        
        // Render the text centered
        int textWidth = textRenderer.getWidth(digit);
        float x = -textWidth / 2.0f;
        float y = -textRenderer.fontHeight / 2.0f;
        
        // Main text with shadow for depth
        textRenderer.draw(
            Text.literal(digit),
            x,
            y,
            color,
            true, // shadow
            matrices.peek().getPositionMatrix(),
            vertexConsumers,
            TextRenderer.TextLayerType.NORMAL,
            0, // background color (transparent)
            15728880 // full bright light
        );
        
        matrices.pop();
    }
}
