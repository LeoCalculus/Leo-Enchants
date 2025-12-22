package com.leo.enchants.client;

import com.leo.enchants.LeoEnchantsMod;
import com.leo.enchants.entity.HerobrineEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

/**
 * Renders Herobrine as a player model with pure black body and glowing white eyes.
 * Includes special effects based on phase and attack state.
 */
public class HerobrineRenderer extends EntityRenderer<HerobrineEntity, HerobrineRenderState> {

    private static final Identifier HEROBRINE_TEXTURE = Identifier.of(LeoEnchantsMod.MOD_ID, "textures/entity/herobrine.png");
    private static final Identifier SWORD_TEXTURE = Identifier.ofVanilla("textures/item/stone_sword.png");
    
    private final PlayerEntityModel model;

    public HerobrineRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.5f;
        ModelPart modelPart = context.getPart(EntityModelLayers.PLAYER);
        this.model = new PlayerEntityModel(modelPart, false);
    }

    @Override
    public HerobrineRenderState createRenderState() {
        return new HerobrineRenderState();
    }

    @Override
    public void updateRenderState(HerobrineEntity entity, HerobrineRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.bodyYaw = entity.bodyYaw;
        state.headYaw = entity.headYaw;
        state.phase = entity.getPhase();
        state.isAttacking = entity.getIsAttacking();
        state.attackType = entity.getAttackType();
        state.attackAnimationTicks = entity.getAttackAnimationTicks();
        state.swordSwingTicks = entity.getSwordSwingTicks();
        state.tickDelta = tickDelta;
        state.isSkyAttack = entity.getIsSkyAttack();
        state.isDying = entity.getIsDying();
        state.deathAnimationTicks = entity.getDeathAnimationTicks();
    }

    @Override
    public void render(HerobrineRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        // During death animation, fade out
        if (state.isDying) {
            float deathProgress = state.deathAnimationTicks / 200.0f; // 10 seconds
            if (deathProgress >= 1.0f) return; // Don't render after death
            
            // Fade out and ascend during death
            matrices.push();
            matrices.translate(0, deathProgress * 5, 0); // Rise up
            matrices.scale(1.0f - deathProgress * 0.5f, 1.0f - deathProgress * 0.5f, 1.0f - deathProgress * 0.5f); // Shrink
        }
        
        // Render giant sword FIRST (behind Herobrine during dive)
        if (state.isSkyAttack && state.swordSwingTicks > 0) {
            renderGiantSword(state, matrices, vertexConsumers, light);
        }
        
        matrices.push();
        
        // Apply rotation based on body yaw
        float bodyYaw = state.bodyYaw;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - bodyYaw));
        
        // Flip the model right-side up
        matrices.scale(0.9375f, -0.9375f, 0.9375f);
        matrices.translate(0.0, -1.501, 0.0);
        
        // Reset model parts
        resetModelParts();
        
        // Apply animations based on state
        applyAnimations(state);
        
        // Use full brightness for the dark figure to stand out
        int fullBright = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        
        // Render the main body with pure black color (fade during death)
        RenderLayer renderLayer = RenderLayer.getEntityTranslucent(HEROBRINE_TEXTURE);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);
        
        // Pure black body color with fade during death
        int alpha = state.isDying ? (int) (255 * (1.0f - state.deathAnimationTicks / 200.0f)) : 255;
        int bodyColor = (alpha << 24) | 0x000000;
        this.model.render(matrices, vertexConsumer, fullBright, OverlayTexture.DEFAULT_UV, bodyColor);
        
        // Render glowing white eyes
        renderGlowingEyes(state, matrices, vertexConsumers);
        
        // Phase-specific effects
        renderPhaseEffects(state, matrices, vertexConsumers);
        
        matrices.pop();
        
        if (state.isDying) {
            matrices.pop();
        }
        
        super.render(state, matrices, vertexConsumers, light);
    }
    
    private void resetModelParts() {
        this.model.head.pitch = 0;
        this.model.head.yaw = 0;
        this.model.head.roll = 0;
        this.model.hat.pitch = 0;
        this.model.hat.yaw = 0;
        this.model.hat.roll = 0;
        this.model.body.pitch = 0;
        this.model.body.yaw = 0;
        this.model.body.roll = 0;
        this.model.leftArm.pitch = 0;
        this.model.leftArm.yaw = 0;
        this.model.leftArm.roll = 0;
        this.model.rightArm.pitch = 0;
        this.model.rightArm.yaw = 0;
        this.model.rightArm.roll = 0;
        this.model.leftLeg.pitch = 0;
        this.model.leftLeg.yaw = 0;
        this.model.leftLeg.roll = 0;
        this.model.rightLeg.pitch = 0;
        this.model.rightLeg.yaw = 0;
        this.model.rightLeg.roll = 0;
    }
    
    private void applyAnimations(HerobrineRenderState state) {
        // Idle floating animation - subtle sway
        float time = state.age * 0.1f;
        
        // Sky attack animation
        if (state.isSkyAttack && state.swordSwingTicks > 0) {
            applySkyAttackAnimation(state);
            return;
        }
        
        // Slight arm sway (when not attacking)
        if (state.swordSwingTicks <= 0) {
            this.model.leftArm.roll = MathHelper.sin(time * 0.5f) * 0.1f - 0.1f;
            this.model.rightArm.roll = -MathHelper.sin(time * 0.5f) * 0.1f + 0.1f;
        }
        
        // Legs slightly apart for floating pose
        this.model.leftLeg.roll = -0.05f;
        this.model.rightLeg.roll = 0.05f;
        
        // Phase-specific poses
        if (state.phase >= 2) {
            // More aggressive stance in later phases
            this.model.body.pitch += 0.1f;
            this.model.head.pitch = -0.1f;
        }
        
        if (state.phase >= 3) {
            // Even more intense
            float intensity = MathHelper.sin(time * 2) * 0.05f;
            this.model.body.pitch += 0.05f + intensity;
        }
    }
    
    /**
     * Apply sky attack animation - diving down with sword
     */
    private void applySkyAttackAnimation(HerobrineRenderState state) {
        float totalTicks = 60.0f; // SKY_SWORD_SWING_DURATION
        float progress = 1.0f - (state.swordSwingTicks / totalTicks);
        
        // Body leaning forward (diving pose)
        this.model.body.pitch = 0.8f + (0.5f * progress);
        this.model.head.pitch = -0.3f;
        
        // Both arms raised holding the sword
        float armRaise = -2.5f + (progress * 1.5f); // Raising up then swinging down
        this.model.rightArm.pitch = armRaise;
        this.model.leftArm.pitch = armRaise;
        this.model.rightArm.roll = -0.3f;
        this.model.leftArm.roll = 0.3f;
        
        // Legs trailing behind
        this.model.leftLeg.pitch = -0.5f - (0.3f * progress);
        this.model.rightLeg.pitch = -0.5f - (0.3f * progress);
    }
    
    /**
     * Renders a giant stone sword using simple quads
     */
    private void renderGiantSword(HerobrineRenderState state, MatrixStack matrices, 
                                   VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        
        float totalTicks = 60.0f;
        float progress = 1.0f - (state.swordSwingTicks / totalTicks);
        
        // Position the sword in Herobrine's hands
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - state.bodyYaw));
        
        // Move to hand position (above Herobrine)
        matrices.translate(0, 2.5, 0);
        
        // Scale the sword to be GIANT (6x normal size)
        float swordScale = 6.0f;
        matrices.scale(swordScale, swordScale, swordScale);
        
        // Rotate sword based on animation progress (swinging down)
        float swordPitch = -60.0f + (progress * 120.0f); // Swings from behind to forward
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(swordPitch));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-45.0f));
        
        // Get the render layer for the sword
        RenderLayer swordLayer = RenderLayer.getEntitySolid(SWORD_TEXTURE);
        VertexConsumer consumer = vertexConsumers.getBuffer(swordLayer);
        
        // Render a simple sword shape using quads
        Matrix4f posMatrix = matrices.peek().getPositionMatrix();
        
        int swordLight = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        
        // Sword blade - dark gray color
        int bladeColor = 0xFF3A3A3A;
        float r = ((bladeColor >> 16) & 0xFF) / 255.0f;
        float g = ((bladeColor >> 8) & 0xFF) / 255.0f;
        float b = (bladeColor & 0xFF) / 255.0f;
        
        // Blade dimensions
        float bladeWidth = 0.15f;
        float bladeLength = 1.2f;
        float bladeThickness = 0.05f;
        
        // Handle dimensions
        float handleWidth = 0.08f;
        float handleLength = 0.35f;
        
        // Draw blade (simple rectangular prism)
        // Front face
        vertex(consumer, posMatrix, -bladeWidth, 0, bladeThickness, r, g, b, 1.0f, 0, 0, swordLight, 0, 0, 1);
        vertex(consumer, posMatrix, bladeWidth, 0, bladeThickness, r, g, b, 1.0f, 1, 0, swordLight, 0, 0, 1);
        vertex(consumer, posMatrix, bladeWidth, bladeLength, bladeThickness, r, g, b, 1.0f, 1, 1, swordLight, 0, 0, 1);
        vertex(consumer, posMatrix, -bladeWidth, bladeLength, bladeThickness, r, g, b, 1.0f, 0, 1, swordLight, 0, 0, 1);
        
        // Back face
        vertex(consumer, posMatrix, bladeWidth, 0, -bladeThickness, r, g, b, 1.0f, 0, 0, swordLight, 0, 0, -1);
        vertex(consumer, posMatrix, -bladeWidth, 0, -bladeThickness, r, g, b, 1.0f, 1, 0, swordLight, 0, 0, -1);
        vertex(consumer, posMatrix, -bladeWidth, bladeLength, -bladeThickness, r, g, b, 1.0f, 1, 1, swordLight, 0, 0, -1);
        vertex(consumer, posMatrix, bladeWidth, bladeLength, -bladeThickness, r, g, b, 1.0f, 0, 1, swordLight, 0, 0, -1);
        
        // Right side
        vertex(consumer, posMatrix, bladeWidth, 0, bladeThickness, r, g, b, 1.0f, 0, 0, swordLight, 1, 0, 0);
        vertex(consumer, posMatrix, bladeWidth, 0, -bladeThickness, r, g, b, 1.0f, 1, 0, swordLight, 1, 0, 0);
        vertex(consumer, posMatrix, bladeWidth, bladeLength, -bladeThickness, r, g, b, 1.0f, 1, 1, swordLight, 1, 0, 0);
        vertex(consumer, posMatrix, bladeWidth, bladeLength, bladeThickness, r, g, b, 1.0f, 0, 1, swordLight, 1, 0, 0);
        
        // Left side
        vertex(consumer, posMatrix, -bladeWidth, 0, -bladeThickness, r, g, b, 1.0f, 0, 0, swordLight, -1, 0, 0);
        vertex(consumer, posMatrix, -bladeWidth, 0, bladeThickness, r, g, b, 1.0f, 1, 0, swordLight, -1, 0, 0);
        vertex(consumer, posMatrix, -bladeWidth, bladeLength, bladeThickness, r, g, b, 1.0f, 1, 1, swordLight, -1, 0, 0);
        vertex(consumer, posMatrix, -bladeWidth, bladeLength, -bladeThickness, r, g, b, 1.0f, 0, 1, swordLight, -1, 0, 0);
        
        // Tip (pointed)
        float tipY = bladeLength + 0.3f;
        vertex(consumer, posMatrix, 0, tipY, 0, r, g, b, 1.0f, 0.5f, 1, swordLight, 0, 1, 0);
        vertex(consumer, posMatrix, -bladeWidth, bladeLength, bladeThickness, r, g, b, 1.0f, 0, 0, swordLight, 0, 1, 0);
        vertex(consumer, posMatrix, bladeWidth, bladeLength, bladeThickness, r, g, b, 1.0f, 1, 0, swordLight, 0, 1, 0);
        vertex(consumer, posMatrix, 0, tipY, 0, r, g, b, 1.0f, 0.5f, 1, swordLight, 0, 1, 0);
        
        // Handle (darker)
        float hr = 0.15f, hg = 0.1f, hb = 0.05f;
        float handleY = -handleLength;
        
        // Handle front
        vertex(consumer, posMatrix, -handleWidth, handleY, handleWidth, hr, hg, hb, 1.0f, 0, 0, swordLight, 0, 0, 1);
        vertex(consumer, posMatrix, handleWidth, handleY, handleWidth, hr, hg, hb, 1.0f, 1, 0, swordLight, 0, 0, 1);
        vertex(consumer, posMatrix, handleWidth, 0, handleWidth, hr, hg, hb, 1.0f, 1, 1, swordLight, 0, 0, 1);
        vertex(consumer, posMatrix, -handleWidth, 0, handleWidth, hr, hg, hb, 1.0f, 0, 1, swordLight, 0, 0, 1);
        
        // Handle back
        vertex(consumer, posMatrix, handleWidth, handleY, -handleWidth, hr, hg, hb, 1.0f, 0, 0, swordLight, 0, 0, -1);
        vertex(consumer, posMatrix, -handleWidth, handleY, -handleWidth, hr, hg, hb, 1.0f, 1, 0, swordLight, 0, 0, -1);
        vertex(consumer, posMatrix, -handleWidth, 0, -handleWidth, hr, hg, hb, 1.0f, 1, 1, swordLight, 0, 0, -1);
        vertex(consumer, posMatrix, handleWidth, 0, -handleWidth, hr, hg, hb, 1.0f, 0, 1, swordLight, 0, 0, -1);
        
        // Cross guard (wider than handle)
        float guardWidth = 0.25f;
        float guardHeight = 0.08f;
        float guardY = 0;
        
        vertex(consumer, posMatrix, -guardWidth, guardY, bladeThickness, r * 0.8f, g * 0.8f, b * 0.8f, 1.0f, 0, 0, swordLight, 0, 0, 1);
        vertex(consumer, posMatrix, guardWidth, guardY, bladeThickness, r * 0.8f, g * 0.8f, b * 0.8f, 1.0f, 1, 0, swordLight, 0, 0, 1);
        vertex(consumer, posMatrix, guardWidth, guardY + guardHeight, bladeThickness, r * 0.8f, g * 0.8f, b * 0.8f, 1.0f, 1, 1, swordLight, 0, 0, 1);
        vertex(consumer, posMatrix, -guardWidth, guardY + guardHeight, bladeThickness, r * 0.8f, g * 0.8f, b * 0.8f, 1.0f, 0, 1, swordLight, 0, 0, 1);
        
        matrices.pop();
    }
    
    private void vertex(VertexConsumer consumer, Matrix4f posMatrix,
                        float x, float y, float z, float r, float g, float b, float a,
                        float u, float v, int light, float nx, float ny, float nz) {
        consumer.vertex(posMatrix, x, y, z)
                .color(r, g, b, a)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(nx, ny, nz);
    }
    
    /**
     * Renders the glowing white eyes using emissive layer.
     */
    private void renderGlowingEyes(HerobrineRenderState state, MatrixStack matrices, 
                                    VertexConsumerProvider vertexConsumers) {
        // Render eyes with emissive glow
        RenderLayer eyeLayer = RenderLayer.getEyes(HEROBRINE_TEXTURE);
        VertexConsumer eyeConsumer = vertexConsumers.getBuffer(eyeLayer);
        
        // Bright white glow for the iconic Herobrine eyes
        int eyeColor = 0xFFFFFFFF; // Pure white
        
        // Pulsing glow effect
        float pulse = (MathHelper.sin(state.age * 0.15f) + 1.0f) * 0.5f;
        int alpha = (int) (200 + pulse * 55);
        eyeColor = (alpha << 24) | 0xFFFFFF;
        
        // More intense glow during sky attack
        if (state.isSkyAttack) {
            eyeColor = 0xFFFF4444; // Red tint during attack
        }
        
        // Render the head with glowing eyes
        this.model.head.render(matrices, eyeConsumer, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, eyeColor);
        this.model.hat.render(matrices, eyeConsumer, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, eyeColor);
    }
    
    /**
     * Renders additional visual effects based on the current phase.
     */
    private void renderPhaseEffects(HerobrineRenderState state, MatrixStack matrices,
                                     VertexConsumerProvider vertexConsumers) {
        // Phase 2+: Add dark aura effect
        if (state.phase >= 2) {
            // Could add additional overlay rendering here
        }
        
        // Phase 3: Intense aura
        if (state.phase >= 3) {
            // Additional intense effects
        }
    }
}
