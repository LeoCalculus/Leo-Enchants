package com.leo.enchants.client;

import com.leo.enchants.LeoEnchantsMod;
import com.leo.enchants.entity.SpaceTravelPortalEntity;
import com.leo.enchants.logic.SpaceTravelHandler;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

/**
 * Renderer for SpaceTravelPortalEntity.
 * Renders a curved surface that shows a preview of the destination dimension.
 * The curved surface is rendered as a series of quads forming a parabolic arc.
 */
public class SpaceTravelPortalRenderer extends EntityRenderer<SpaceTravelPortalEntity, SpaceTravelPortalRenderState> {
    
    private static final Identifier PORTAL_TEXTURE = Identifier.of(LeoEnchantsMod.MOD_ID, "textures/entity/space_portal.png");
    
    // Portal dimensions
    private static final float WIDTH = SpaceTravelPortalEntity.PORTAL_WIDTH;
    private static final float HEIGHT = SpaceTravelPortalEntity.PORTAL_HEIGHT;
    private static final float CURVE_DEPTH = SpaceTravelPortalEntity.PORTAL_CURVE_DEPTH;
    private static final int CURVE_SEGMENTS = 16;
    
    public SpaceTravelPortalRenderer(EntityRendererFactory.Context context) {
        super(context);
    }
    
    @Override
    public SpaceTravelPortalRenderState createRenderState() {
        return new SpaceTravelPortalRenderState();
    }
    
    @Override
    public void updateRenderState(SpaceTravelPortalEntity entity, SpaceTravelPortalRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.targetDimension = entity.getTargetDimension();
        state.portalYaw = entity.getPortalYaw();
        state.portalPitch = entity.getPortalPitch();
        state.closingTicks = entity.getClosingTicks();
        state.isClosing = entity.isClosing();
        state.lifetimeTicks = entity.getLifetimeTicks();
        state.tickDelta = tickDelta;
    }
    
    @Override
    public void render(SpaceTravelPortalRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        
        // Rotate to face the player's look direction
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-state.portalYaw));
        
        // Calculate closing animation scale
        float scale = 1.0f;
        if (state.isClosing) {
            float closingProgress = (state.closingTicks + state.tickDelta) / 60.0f; // 3 seconds = 60 ticks
            scale = Math.max(0.0f, 1.0f - closingProgress);
        }
        
        // Apply scale
        matrices.scale(scale, scale, scale);
        
        // Get dimension color
        int[] color = SpaceTravelHandler.getDimensionColor(state.targetDimension);
        float r = color[0] / 255.0f;
        float g = color[1] / 255.0f;
        float b = color[2] / 255.0f;
        
        // Calculate animated alpha with wave effect
        float time = (state.lifetimeTicks + state.tickDelta) * 0.05f;
        float baseAlpha = 0.7f + 0.2f * (float) Math.sin(time);
        
        // Render the curved portal surface
        renderCurvedSurface(matrices, vertexConsumers, r, g, b, baseAlpha, state.lifetimeTicks + state.tickDelta);
        
        // Render edge glow effect
        renderEdgeGlow(matrices, vertexConsumers, r, g, b, state.lifetimeTicks + state.tickDelta);
        
        matrices.pop();
    }
    
    private void renderCurvedSurface(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                      float r, float g, float b, float alpha, float time) {
        // Use a translucent render layer
        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getTranslucentMovingBlock());
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        float halfWidth = WIDTH / 2;
        float halfHeight = HEIGHT / 2;
        
        // Render curved surface as multiple horizontal strips
        for (int i = 0; i < CURVE_SEGMENTS; i++) {
            float u0 = (float) i / CURVE_SEGMENTS;
            float u1 = (float) (i + 1) / CURVE_SEGMENTS;
            
            // Map u to angle (-PI/2 to PI/2 for a semicircle curve)
            float angle0 = (u0 - 0.5f) * (float) Math.PI;
            float angle1 = (u1 - 0.5f) * (float) Math.PI;
            
            // Calculate curve positions (parabolic/semicircular curve)
            float z0 = (float) Math.cos(angle0) * CURVE_DEPTH;
            float x0 = (float) Math.sin(angle0) * halfWidth;
            float z1 = (float) Math.cos(angle1) * CURVE_DEPTH;
            float x1 = (float) Math.sin(angle1) * halfWidth;
            
            // Add wave animation
            float wave0 = (float) Math.sin(time * 0.1f + angle0 * 2) * 0.05f;
            float wave1 = (float) Math.sin(time * 0.1f + angle1 * 2) * 0.05f;
            z0 += wave0;
            z1 += wave1;
            
            // Vary color along the curve for depth effect
            float brightness0 = 0.7f + 0.3f * (float) Math.cos(angle0);
            float brightness1 = 0.7f + 0.3f * (float) Math.cos(angle1);
            
            // Draw quad for this segment
            int color0 = getPackedColor(r * brightness0, g * brightness0, b * brightness0, alpha);
            int color1 = getPackedColor(r * brightness1, g * brightness1, b * brightness1, alpha);
            
            // Front face
            consumer.vertex(matrix, x0, -halfHeight, z0).color(color0).texture(u0, 1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 0, 1);
            consumer.vertex(matrix, x1, -halfHeight, z1).color(color1).texture(u1, 1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 0, 1);
            consumer.vertex(matrix, x1, halfHeight, z1).color(color1).texture(u1, 0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 0, 1);
            consumer.vertex(matrix, x0, halfHeight, z0).color(color0).texture(u0, 0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 0, 1);
            
            // Back face (for visibility from both sides)
            consumer.vertex(matrix, x0, halfHeight, z0).color(color0).texture(u0, 0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 0, -1);
            consumer.vertex(matrix, x1, halfHeight, z1).color(color1).texture(u1, 0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 0, -1);
            consumer.vertex(matrix, x1, -halfHeight, z1).color(color1).texture(u1, 1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 0, -1);
            consumer.vertex(matrix, x0, -halfHeight, z0).color(color0).texture(u0, 1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 0, -1);
        }
    }
    
    private void renderEdgeGlow(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                 float r, float g, float b, float time) {
        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getTranslucentMovingBlock());
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        float halfWidth = WIDTH / 2;
        float halfHeight = HEIGHT / 2;
        float glowSize = 0.15f;
        
        // Animate glow intensity
        float glowPulse = 0.5f + 0.5f * (float) Math.sin(time * 0.15f);
        float glowAlpha = 0.4f * glowPulse;
        
        int glowColor = getPackedColor(r, g, b, glowAlpha);
        int glowColorFade = getPackedColor(r, g, b, 0);
        
        // Render glow around the edge of the curve
        for (int i = 0; i < CURVE_SEGMENTS; i++) {
            float u0 = (float) i / CURVE_SEGMENTS;
            float u1 = (float) (i + 1) / CURVE_SEGMENTS;
            
            float angle0 = (u0 - 0.5f) * (float) Math.PI;
            float angle1 = (u1 - 0.5f) * (float) Math.PI;
            
            float z0 = (float) Math.cos(angle0) * CURVE_DEPTH;
            float x0 = (float) Math.sin(angle0) * halfWidth;
            float z1 = (float) Math.cos(angle1) * CURVE_DEPTH;
            float x1 = (float) Math.sin(angle1) * halfWidth;
            
            float zOuter0 = (float) Math.cos(angle0) * (CURVE_DEPTH + glowSize);
            float xOuter0 = (float) Math.sin(angle0) * (halfWidth + glowSize);
            float zOuter1 = (float) Math.cos(angle1) * (CURVE_DEPTH + glowSize);
            float xOuter1 = (float) Math.sin(angle1) * (halfWidth + glowSize);
            
            // Top edge glow
            consumer.vertex(matrix, x0, halfHeight, z0).color(glowColor).texture(0, 0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 1, 0);
            consumer.vertex(matrix, x1, halfHeight, z1).color(glowColor).texture(1, 0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 1, 0);
            consumer.vertex(matrix, xOuter1, halfHeight + glowSize, zOuter1).color(glowColorFade).texture(1, 1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 1, 0);
            consumer.vertex(matrix, xOuter0, halfHeight + glowSize, zOuter0).color(glowColorFade).texture(0, 1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 1, 0);
            
            // Bottom edge glow
            consumer.vertex(matrix, xOuter0, -halfHeight - glowSize, zOuter0).color(glowColorFade).texture(0, 0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, -1, 0);
            consumer.vertex(matrix, xOuter1, -halfHeight - glowSize, zOuter1).color(glowColorFade).texture(1, 0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, -1, 0);
            consumer.vertex(matrix, x1, -halfHeight, z1).color(glowColor).texture(1, 1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, -1, 0);
            consumer.vertex(matrix, x0, -halfHeight, z0).color(glowColor).texture(0, 1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, -1, 0);
        }
    }
    
    private int getPackedColor(float r, float g, float b, float a) {
        int ri = (int) (r * 255) & 0xFF;
        int gi = (int) (g * 255) & 0xFF;
        int bi = (int) (b * 255) & 0xFF;
        int ai = (int) (a * 255) & 0xFF;
        return (ai << 24) | (bi << 16) | (gi << 8) | ri;
    }
}

