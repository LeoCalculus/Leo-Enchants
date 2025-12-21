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
 * Renders a curved portal surface with an end-portal-style dimensional preview effect.
 * The effect shows a swirling dimensional rift that hints at the destination dimension.
 */
public class SpaceTravelPortalRenderer extends EntityRenderer<SpaceTravelPortalEntity, SpaceTravelPortalRenderState> {
    
    private static final Identifier END_SKY_TEXTURE = Identifier.ofVanilla("textures/environment/end_sky.png");
    private static final Identifier END_PORTAL_TEXTURE = Identifier.ofVanilla("textures/entity/end_portal.png");
    
    // Portal dimensions
    private static final float WIDTH = SpaceTravelPortalEntity.PORTAL_WIDTH;
    private static final float HEIGHT = SpaceTravelPortalEntity.PORTAL_HEIGHT;
    private static final float CURVE_DEPTH = SpaceTravelPortalEntity.PORTAL_CURVE_DEPTH;
    private static final int CURVE_SEGMENTS = 24; // Higher for smoother curve
    private static final int PORTAL_LAYERS = 16; // Number of overlapping effect layers
    
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
        
        // Apply scale with slight wobble during close
        if (state.isClosing) {
            float wobble = (float) Math.sin(state.closingTicks * 0.5f) * 0.1f * (1 - scale);
            matrices.scale(scale + wobble, scale, scale + wobble);
        } else {
            matrices.scale(scale, scale, scale);
        }
        
        float time = (state.lifetimeTicks + state.tickDelta);
        
        // Get dimension-specific colors
        float[] baseColor = getDimensionBaseColor(state.targetDimension);
        float[] accentColor = getDimensionAccentColor(state.targetDimension);
        
        // Render the dimensional rift effect (multiple layers like end portal)
        renderDimensionalRift(matrices, vertexConsumers, baseColor, accentColor, time, state.targetDimension);
        
        // Render outer edge glow
        renderEdgeGlow(matrices, vertexConsumers, accentColor, time);
        
        // Render swirling particles on surface
        renderSurfaceDetails(matrices, vertexConsumers, baseColor, accentColor, time);
        
        matrices.pop();
    }
    
    /**
     * Render the main dimensional rift effect with multiple overlapping layers
     */
    private void renderDimensionalRift(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                        float[] baseColor, float[] accentColor, float time, String dimension) {
        
        float halfWidth = WIDTH / 2;
        float halfHeight = HEIGHT / 2;
        
        // Render multiple layers with depth effect (similar to end portal)
        for (int layer = 0; layer < PORTAL_LAYERS; layer++) {
            float layerDepth = (float) layer / PORTAL_LAYERS;
            float layerOffset = layerDepth * 0.3f; // Layers recede into portal
            
            // Calculate layer-specific animation
            float layerTime = time * (0.02f + layer * 0.005f);
            float layerAlpha = 0.15f + (1.0f - layerDepth) * 0.4f;
            
            // Alternate between base and accent colors with depth
            float colorMix = (float) Math.sin(layerTime + layer * 0.5f) * 0.5f + 0.5f;
            float r = baseColor[0] * (1 - colorMix) + accentColor[0] * colorMix;
            float g = baseColor[1] * (1 - colorMix) + accentColor[1] * colorMix;
            float b = baseColor[2] * (1 - colorMix) + accentColor[2] * colorMix;
            
            // Add shimmer effect
            float shimmer = 0.8f + 0.2f * (float) Math.sin(layerTime * 2 + layer);
            r *= shimmer;
            g *= shimmer;
            b *= shimmer;
            
            renderCurvedLayer(matrices, vertexConsumers, halfWidth, halfHeight, 
                              layerOffset, r, g, b, layerAlpha, layerTime, layer);
        }
    }
    
    /**
     * Render a single curved layer of the portal
     */
    private void renderCurvedLayer(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                    float halfWidth, float halfHeight, float depthOffset,
                                    float r, float g, float b, float alpha, float time, int layer) {
        
        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getTranslucentMovingBlock());
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        // UV animation for swirling effect
        float uvOffsetX = (float) Math.sin(time + layer * 0.3f) * 0.1f;
        float uvOffsetY = (float) Math.cos(time * 0.7f + layer * 0.2f) * 0.1f;
        float uvScale = 1.0f + layer * 0.1f;
        
        for (int i = 0; i < CURVE_SEGMENTS; i++) {
            float u0 = (float) i / CURVE_SEGMENTS;
            float u1 = (float) (i + 1) / CURVE_SEGMENTS;
            
            // Map u to angle for curved surface
            float angle0 = (u0 - 0.5f) * (float) Math.PI;
            float angle1 = (u1 - 0.5f) * (float) Math.PI;
            
            // Calculate curve positions with depth offset
            float effectiveDepth = CURVE_DEPTH - depthOffset;
            float z0 = (float) Math.cos(angle0) * effectiveDepth;
            float x0 = (float) Math.sin(angle0) * halfWidth;
            float z1 = (float) Math.cos(angle1) * effectiveDepth;
            float x1 = (float) Math.sin(angle1) * halfWidth;
            
            // Add subtle wave animation
            float wave0 = (float) Math.sin(time * 0.08f + angle0 * 3 + layer * 0.2f) * 0.03f;
            float wave1 = (float) Math.sin(time * 0.08f + angle1 * 3 + layer * 0.2f) * 0.03f;
            z0 += wave0;
            z1 += wave1;
            
            // Calculate animated UVs for swirl effect
            float texU0 = (u0 * uvScale + uvOffsetX) % 1.0f;
            float texU1 = (u1 * uvScale + uvOffsetX) % 1.0f;
            float texV0 = (0.0f + uvOffsetY) % 1.0f;
            float texV1 = (1.0f + uvOffsetY) % 1.0f;
            
            // Brightness variation across curve
            float brightness0 = 0.6f + 0.4f * (float) Math.cos(angle0);
            float brightness1 = 0.6f + 0.4f * (float) Math.cos(angle1);
            
            int color0 = getPackedColor(r * brightness0, g * brightness0, b * brightness0, alpha * brightness0);
            int color1 = getPackedColor(r * brightness1, g * brightness1, b * brightness1, alpha * brightness1);
            
            // Front face
            consumer.vertex(matrix, x0, -halfHeight, z0).color(color0).texture(texU0, texV1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 0, 1);
            consumer.vertex(matrix, x1, -halfHeight, z1).color(color1).texture(texU1, texV1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 0, 1);
            consumer.vertex(matrix, x1, halfHeight, z1).color(color1).texture(texU1, texV0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 0, 1);
            consumer.vertex(matrix, x0, halfHeight, z0).color(color0).texture(texU0, texV0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 0, 1);
            
            // Back face
            consumer.vertex(matrix, x0, halfHeight, z0).color(color0).texture(texU0, texV0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 0, -1);
            consumer.vertex(matrix, x1, halfHeight, z1).color(color1).texture(texU1, texV0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 0, -1);
            consumer.vertex(matrix, x1, -halfHeight, z1).color(color1).texture(texU1, texV1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 0, -1);
            consumer.vertex(matrix, x0, -halfHeight, z0).color(color0).texture(texU0, texV1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 0, -1);
        }
    }
    
    /**
     * Render glowing edge around the portal
     */
    private void renderEdgeGlow(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                 float[] color, float time) {
        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getTranslucentMovingBlock());
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        float halfWidth = WIDTH / 2;
        float halfHeight = HEIGHT / 2;
        float glowSize = 0.2f;
        
        // Pulsing glow
        float glowPulse = 0.6f + 0.4f * (float) Math.sin(time * 0.12f);
        float glowAlpha = 0.5f * glowPulse;
        
        int glowColor = getPackedColor(color[0], color[1], color[2], glowAlpha);
        int glowColorFade = getPackedColor(color[0], color[1], color[2], 0);
        
        // Render glow strips along the curved edge
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
        
        // Side edge glows
        float leftAngle = -0.5f * (float) Math.PI;
        float rightAngle = 0.5f * (float) Math.PI;
        
        float leftZ = (float) Math.cos(leftAngle) * CURVE_DEPTH;
        float leftX = (float) Math.sin(leftAngle) * halfWidth;
        float leftZOuter = (float) Math.cos(leftAngle) * (CURVE_DEPTH + glowSize);
        float leftXOuter = (float) Math.sin(leftAngle) * (halfWidth + glowSize);
        
        // Left side glow
        consumer.vertex(matrix, leftX, -halfHeight, leftZ).color(glowColor).texture(0, 0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(-1, 0, 0);
        consumer.vertex(matrix, leftX, halfHeight, leftZ).color(glowColor).texture(0, 1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(-1, 0, 0);
        consumer.vertex(matrix, leftXOuter, halfHeight + glowSize, leftZOuter).color(glowColorFade).texture(1, 1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(-1, 0, 0);
        consumer.vertex(matrix, leftXOuter, -halfHeight - glowSize, leftZOuter).color(glowColorFade).texture(1, 0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(-1, 0, 0);
        
        float rightZ = (float) Math.cos(rightAngle) * CURVE_DEPTH;
        float rightX = (float) Math.sin(rightAngle) * halfWidth;
        float rightZOuter = (float) Math.cos(rightAngle) * (CURVE_DEPTH + glowSize);
        float rightXOuter = (float) Math.sin(rightAngle) * (halfWidth + glowSize);
        
        // Right side glow
        consumer.vertex(matrix, rightXOuter, -halfHeight - glowSize, rightZOuter).color(glowColorFade).texture(0, 0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(1, 0, 0);
        consumer.vertex(matrix, rightXOuter, halfHeight + glowSize, rightZOuter).color(glowColorFade).texture(0, 1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(1, 0, 0);
        consumer.vertex(matrix, rightX, halfHeight, rightZ).color(glowColor).texture(1, 1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(1, 0, 0);
        consumer.vertex(matrix, rightX, -halfHeight, rightZ).color(glowColor).texture(1, 0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(1, 0, 0);
    }
    
    /**
     * Render swirling surface details for added visual depth
     */
    private void renderSurfaceDetails(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                       float[] baseColor, float[] accentColor, float time) {
        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getTranslucentMovingBlock());
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        float halfWidth = WIDTH / 2;
        float halfHeight = HEIGHT / 2;
        
        // Render swirling "stars" or energy points on the surface
        int numDetails = 20;
        for (int i = 0; i < numDetails; i++) {
            float seed = i * 1.618033988f; // Golden ratio for distribution
            
            // Animated position on the curved surface
            float u = (float) ((seed + time * 0.02f * (1 + (i % 3) * 0.3f)) % 1.0f);
            float v = (float) ((seed * 2.3f + time * 0.015f * (1 + (i % 2) * 0.2f)) % 1.0f);
            
            float angle = (u - 0.5f) * (float) Math.PI;
            float z = (float) Math.cos(angle) * (CURVE_DEPTH - 0.05f);
            float x = (float) Math.sin(angle) * halfWidth;
            float y = (v - 0.5f) * HEIGHT;
            
            // Pulsing size and alpha
            float pulse = 0.5f + 0.5f * (float) Math.sin(time * 0.2f + seed * 10);
            float size = 0.05f + 0.03f * pulse;
            float alpha = 0.3f + 0.4f * pulse;
            
            // Alternate colors
            float[] color = (i % 2 == 0) ? accentColor : baseColor;
            int packedColor = getPackedColor(color[0], color[1], color[2], alpha);
            int fadedColor = getPackedColor(color[0], color[1], color[2], 0);
            
            // Render as small quad
            consumer.vertex(matrix, x - size, y - size, z).color(fadedColor).texture(0, 0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 0, 1);
            consumer.vertex(matrix, x + size, y - size, z).color(fadedColor).texture(1, 0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 0, 1);
            consumer.vertex(matrix, x + size, y + size, z).color(packedColor).texture(1, 1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 0, 1);
            consumer.vertex(matrix, x - size, y + size, z).color(packedColor).texture(0, 1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 0, 1);
        }
    }
    
    /**
     * Get base color for dimension (darker, background color)
     */
    private float[] getDimensionBaseColor(String dimensionId) {
        if (dimensionId.contains("nether")) {
            return new float[]{0.6f, 0.15f, 0.05f}; // Deep red/orange
        } else if (dimensionId.contains("end")) {
            return new float[]{0.1f, 0.05f, 0.2f}; // Deep purple/black
        } else {
            return new float[]{0.1f, 0.3f, 0.5f}; // Deep blue/sky
        }
    }
    
    /**
     * Get accent color for dimension (brighter, highlight color)
     */
    private float[] getDimensionAccentColor(String dimensionId) {
        if (dimensionId.contains("nether")) {
            return new float[]{1.0f, 0.5f, 0.2f}; // Bright orange/fire
        } else if (dimensionId.contains("end")) {
            return new float[]{0.8f, 0.4f, 1.0f}; // Bright purple/pink
        } else {
            return new float[]{0.5f, 0.9f, 1.0f}; // Bright cyan/sky blue
        }
    }
    
    private int getPackedColor(float r, float g, float b, float a) {
        int ri = Math.min(255, Math.max(0, (int) (r * 255))) & 0xFF;
        int gi = Math.min(255, Math.max(0, (int) (g * 255))) & 0xFF;
        int bi = Math.min(255, Math.max(0, (int) (b * 255))) & 0xFF;
        int ai = Math.min(255, Math.max(0, (int) (a * 255))) & 0xFF;
        return (ai << 24) | (bi << 16) | (gi << 8) | ri;
    }
}
