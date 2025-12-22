package com.leo.enchants.client;

import com.leo.enchants.LeoEnchantsMod;
import com.leo.enchants.entity.HerobrineEntity;
import net.minecraft.client.MinecraftClient;
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
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
    private final ItemRenderer itemRenderer;

    public HerobrineRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.5f;
        ModelPart modelPart = context.getPart(EntityModelLayers.PLAYER);
        this.model = new PlayerEntityModel(modelPart, false);
        this.itemRenderer = MinecraftClient.getInstance().getItemRenderer();
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
     * Renders a giant stone sword using the vanilla model with a fan-shaped swing animation.
     * Rotates around the end of the handle as the pivot.
     */
    private void renderGiantSword(HerobrineRenderState state, MatrixStack matrices, 
                                   VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        
        float totalTicks = 60.0f;
        float progress = 1.0f - (state.swordSwingTicks / totalTicks);
        
        // 1. Align with Herobrine's orientation
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - state.bodyYaw));
        
        // 2. Move to the right shoulder pivot point
        // In model coordinates, the shoulder is roughly at these offsets
        matrices.translate(-0.3125f, 1.4f, 0.0f);

        // 3. Apply the fan-shaped swing rotation (-45 to -135 degrees)
        // Rotating around Z axis in model space for a side-to-side/up-to-down sweeping fan
        float swingAngle = -45.0f + (progress * -90.0f); 
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(swingAngle));
        
        // 4. Scale to GIANT size
        float swordScale = 6.0f;
        matrices.scale(swordScale, swordScale, swordScale);
        
        // 5. Transform the sword model so its handle end is at the pivot (0,0,0)
        // Standard item models are 16x16 pixels. The handle end is usually at the bottom-left.
        // We need to move the handle end to our current origin.
        // Vanilla sword model rotation for hand:
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f));
        
        // Adjust translation to put handle end at pivot point
        // These values might need fine-tuning to perfectly match the handle tip
        matrices.translate(0.0f, -0.4f, 0.0f);
        
        // Render the vanilla stone sword item
        ItemStack stoneSword = new ItemStack(Items.STONE_SWORD);
        this.itemRenderer.renderItem(
            stoneSword,
            ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
            light,
            OverlayTexture.DEFAULT_UV,
            matrices,
            vertexConsumers,
            null,
            0
        );
        
        matrices.pop();
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
