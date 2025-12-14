package com.leo.enchants.client;

import com.leo.enchants.entity.GiantSwordEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public class GiantSwordEntityRenderer extends EntityRenderer<GiantSwordEntity, GiantSwordRenderState> {
    
    // Use a simple colored rendering (no texture needed)
    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/misc/white.png");
    
    public GiantSwordEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }
    
    @Override
    public GiantSwordRenderState createRenderState() {
        return new GiantSwordRenderState();
    }
    
    @Override
    public void updateRenderState(GiantSwordEntity entity, GiantSwordRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.swordSize = entity.getSwordSize();
        state.level = entity.getLevel();
        state.hasLanded = entity.getHasLanded();
        state.ticksExisted = entity.getTicksExisted() + tickDelta;
        state.swordType = entity.getSwordType();
    }
    
    @Override
    public void render(GiantSwordRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        
        float size = state.swordSize;
        float scale = size / 50.0f; // Base scale factor
        
        // Rotate the sword to point downward
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
        
        // Scale based on enchantment level
        matrices.scale(scale, scale, scale);
        
        // Only rotate while falling - stop when landed (sword stuck in ground)
        if (!state.hasLanded) {
            float rotation = state.ticksExisted * 2.0f;
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation % 360));
        }
        
        // Pulsing glow effect based on whether it has landed
        float pulseAlpha = state.hasLanded ? 
            0.8f + 0.2f * MathHelper.sin(state.ticksExisted * 0.2f) :
            1.0f;
        
        // Get color based on sword type
        float[] color = getColorForSwordType(state.swordType);
        
        // Render the sword shape
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE));
        renderSwordModel(matrices, vertexConsumer, light, color[0], color[1], color[2], pulseAlpha);
        
        // Render glow effect
        VertexConsumer glowConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(TEXTURE));
        matrices.scale(1.05f, 1.05f, 1.05f);
        renderSwordModel(matrices, glowConsumer, 15728880, color[0] * 0.5f, color[1] * 0.5f, color[2] * 0.5f, pulseAlpha * 0.5f);
        
        matrices.pop();
    }
    
    private float[] getColorForSwordType(String swordType) {
        // Return colors based on sword material - matching Minecraft's actual item colors
        if (swordType.contains("netherite")) {
            return new float[]{0.3f, 0.25f, 0.25f};  // Dark brownish-gray (Netherite)
        } else if (swordType.contains("diamond")) {
            return new float[]{0.4f, 0.9f, 0.9f};    // Cyan/Diamond blue
        } else if (swordType.contains("golden") || swordType.contains("gold")) {
            return new float[]{1.0f, 0.85f, 0.2f};   // Gold
        } else if (swordType.contains("iron")) {
            return new float[]{0.85f, 0.85f, 0.85f}; // Silver/Iron gray
        } else if (swordType.contains("stone")) {
            return new float[]{0.5f, 0.5f, 0.5f};    // Stone gray
        } else if (swordType.contains("wooden") || swordType.contains("wood")) {
            return new float[]{0.6f, 0.4f, 0.2f};    // Brown wood
        } else {
            return new float[]{0.7f, 0.7f, 1.0f};    // Default light blue
        }
    }
    
    private void renderSwordModel(MatrixStack matrices, VertexConsumer vertexConsumer, int light, 
                                   float r, float g, float b, float a) {
        MatrixStack.Entry entry = matrices.peek();
        
        // Sword dimensions (relative to scale)
        float bladeWidth = 0.8f;
        float bladeLength = 25.0f;
        float bladeThickness = 0.3f;
        
        float handleWidth = 0.3f;
        float handleLength = 5.0f;
        
        float guardWidth = 3.0f;
        float guardHeight = 0.5f;
        float guardThickness = 0.4f;
        
        // Render blade (main body)
        renderBox(entry, vertexConsumer, 
            -bladeWidth/2, 0, -bladeThickness/2,
            bladeWidth/2, bladeLength, bladeThickness/2,
            r, g, b, a, light);
        
        // Render blade tip (pointed)
        renderPyramid(entry, vertexConsumer,
            0, bladeLength, 0,
            bladeWidth/2, 5.0f,
            r, g, b, a, light);
        
        // Render guard (crosspiece)
        renderBox(entry, vertexConsumer,
            -guardWidth/2, -guardHeight, -guardThickness/2,
            guardWidth/2, 0, guardThickness/2,
            r * 0.6f, g * 0.6f, b * 0.6f, a, light);
        
        // Render handle
        renderBox(entry, vertexConsumer,
            -handleWidth/2, -guardHeight - handleLength, -handleWidth/2,
            handleWidth/2, -guardHeight, handleWidth/2,
            r * 0.4f, g * 0.4f, b * 0.4f, a, light);
        
        // Render pommel
        renderBox(entry, vertexConsumer,
            -handleWidth, -guardHeight - handleLength - 0.5f, -handleWidth,
            handleWidth, -guardHeight - handleLength, handleWidth,
            r * 0.5f, g * 0.5f, b * 0.5f, a, light);
    }
    
    private void renderBox(MatrixStack.Entry entry, VertexConsumer vertexConsumer,
                          float x1, float y1, float z1, float x2, float y2, float z2,
                          float r, float g, float b, float a, int light) {
        // Front face
        addQuad(entry, vertexConsumer, 
            x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, 
            0, 0, 1, r, g, b, a, light);
        // Back face
        addQuad(entry, vertexConsumer,
            x2, y1, z1, x1, y1, z1, x1, y2, z1, x2, y2, z1,
            0, 0, -1, r, g, b, a, light);
        // Left face
        addQuad(entry, vertexConsumer,
            x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1,
            -1, 0, 0, r, g, b, a, light);
        // Right face
        addQuad(entry, vertexConsumer,
            x2, y1, z2, x2, y1, z1, x2, y2, z1, x2, y2, z2,
            1, 0, 0, r, g, b, a, light);
        // Top face
        addQuad(entry, vertexConsumer,
            x1, y2, z2, x2, y2, z2, x2, y2, z1, x1, y2, z1,
            0, 1, 0, r, g, b, a, light);
        // Bottom face
        addQuad(entry, vertexConsumer,
            x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2,
            0, -1, 0, r, g, b, a, light);
    }
    
    private void renderPyramid(MatrixStack.Entry entry, VertexConsumer vertexConsumer,
                               float tipX, float tipY, float tipZ, float baseHalfWidth, float height,
                               float r, float g, float b, float a, int light) {
        float baseY = tipY;
        float peakY = tipY + height;
        
        // Four triangular faces
        // Front
        addTriangle(entry, vertexConsumer,
            tipX, peakY, tipZ,
            tipX - baseHalfWidth, baseY, tipZ + baseHalfWidth * 0.3f,
            tipX + baseHalfWidth, baseY, tipZ + baseHalfWidth * 0.3f,
            0, 0.5f, 0.5f, r, g, b, a, light);
        // Back
        addTriangle(entry, vertexConsumer,
            tipX, peakY, tipZ,
            tipX + baseHalfWidth, baseY, tipZ - baseHalfWidth * 0.3f,
            tipX - baseHalfWidth, baseY, tipZ - baseHalfWidth * 0.3f,
            0, 0.5f, -0.5f, r, g, b, a, light);
        // Left
        addTriangle(entry, vertexConsumer,
            tipX, peakY, tipZ,
            tipX - baseHalfWidth, baseY, tipZ - baseHalfWidth * 0.3f,
            tipX - baseHalfWidth, baseY, tipZ + baseHalfWidth * 0.3f,
            -0.5f, 0.5f, 0, r, g, b, a, light);
        // Right
        addTriangle(entry, vertexConsumer,
            tipX, peakY, tipZ,
            tipX + baseHalfWidth, baseY, tipZ + baseHalfWidth * 0.3f,
            tipX + baseHalfWidth, baseY, tipZ - baseHalfWidth * 0.3f,
            0.5f, 0.5f, 0, r, g, b, a, light);
    }
    
    private void addQuad(MatrixStack.Entry entry, VertexConsumer vertexConsumer,
                         float x1, float y1, float z1, float x2, float y2, float z2,
                         float x3, float y3, float z3, float x4, float y4, float z4,
                         float nx, float ny, float nz, float r, float g, float b, float a, int light) {
        addVertex(entry, vertexConsumer, x1, y1, z1, nx, ny, nz, r, g, b, a, 0, 0, light);
        addVertex(entry, vertexConsumer, x2, y2, z2, nx, ny, nz, r, g, b, a, 1, 0, light);
        addVertex(entry, vertexConsumer, x3, y3, z3, nx, ny, nz, r, g, b, a, 1, 1, light);
        addVertex(entry, vertexConsumer, x4, y4, z4, nx, ny, nz, r, g, b, a, 0, 1, light);
    }
    
    private void addTriangle(MatrixStack.Entry entry, VertexConsumer vertexConsumer,
                             float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3,
                             float nx, float ny, float nz, float r, float g, float b, float a, int light) {
        addVertex(entry, vertexConsumer, x1, y1, z1, nx, ny, nz, r, g, b, a, 0.5f, 0, light);
        addVertex(entry, vertexConsumer, x2, y2, z2, nx, ny, nz, r, g, b, a, 0, 1, light);
        addVertex(entry, vertexConsumer, x3, y3, z3, nx, ny, nz, r, g, b, a, 1, 1, light);
        // Add fourth vertex to complete quad (duplicate last vertex for triangle)
        addVertex(entry, vertexConsumer, x3, y3, z3, nx, ny, nz, r, g, b, a, 1, 1, light);
    }
    
    private void addVertex(MatrixStack.Entry entry, VertexConsumer vertexConsumer,
                          float x, float y, float z, float nx, float ny, float nz,
                          float r, float g, float b, float a, float u, float v, int light) {
        vertexConsumer.vertex(entry, x, y, z)
            .color(r, g, b, a)
            .texture(u, v)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry, nx, ny, nz);
    }
}
