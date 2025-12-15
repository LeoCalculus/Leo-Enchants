package com.leo.enchants.client;

import com.leo.enchants.LeoEnchantsMod;
import com.leo.enchants.entity.ShadowCloneEntity;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * Renders shadow clones as pure black player models with glowing red eyes.
 * Creates a sinister silhouette effect for the shadow assassin ability.
 */
public class ShadowCloneRenderer extends EntityRenderer<ShadowCloneEntity, ShadowCloneRenderState> {

    private static final Identifier SHADOW_TEXTURE = Identifier.of(LeoEnchantsMod.MOD_ID, "textures/entity/shadow_clone.png");
    
    private final PlayerEntityModel model;
    private final ItemRenderer itemRenderer;

    public ShadowCloneRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.4f;
        // Use the player model for proper player-like appearance
        ModelPart modelPart = context.getPart(EntityModelLayers.PLAYER);
        this.model = new PlayerEntityModel(modelPart, false);
        this.itemRenderer = MinecraftClient.getInstance().getItemRenderer();
    }

    @Override
    public ShadowCloneRenderState createRenderState() {
        return new ShadowCloneRenderState();
    }

    @Override
    public void updateRenderState(ShadowCloneEntity entity, ShadowCloneRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.bodyYaw = entity.getSyncedBodyYaw();
        state.headYaw = entity.getSyncedHeadYaw();
        state.limbSwing = entity.getLimbSwing();
        state.limbSwingAmount = 1.0f;
        state.weaponStack = entity.getWeaponStack();
        state.ageTicks = entity.getAgeTicks();
        state.yawDegrees = entity.getYaw();
        state.entityPitch = entity.getPitch();
        // Lerp tick delta for smooth movement
        state.tickDelta = tickDelta;
    }

    @Override
    public void render(ShadowCloneRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        
        // Apply rotation based on body yaw (face the direction of movement)
        float bodyYaw = state.bodyYaw;
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - bodyYaw));
        
        // Flip the model right-side up (Minecraft models render upside down by default)
        // Scale -1 on Y axis flips the model, then translate to position feet at ground
        matrices.scale(0.9375f, -0.9375f, 0.9375f);
        matrices.translate(0.0, -1.501, 0.0);
        
        // Animate limbs for aggressive running/charging motion
        float animTime = state.ageTicks + state.tickDelta;
        float limbSwing = animTime * 0.8f;  // Faster, more aggressive movement
        float limbSwingAmount = 1.0f;
        
        // Reset all parts first
        resetModelParts();
        
        // Aggressive running animation - arms and legs swinging wide
        this.model.leftLeg.pitch = MathHelper.cos(limbSwing * 0.6662f) * 1.6f * limbSwingAmount;
        this.model.rightLeg.pitch = MathHelper.cos(limbSwing * 0.6662f + (float)Math.PI) * 1.6f * limbSwingAmount;
        this.model.leftArm.pitch = MathHelper.cos(limbSwing * 0.6662f + (float)Math.PI) * 1.2f * limbSwingAmount;
        this.model.rightArm.pitch = MathHelper.cos(limbSwing * 0.6662f) * 1.2f * limbSwingAmount;
        
        // Right arm raised with weapon - attacking pose
        this.model.rightArm.pitch = -1.2f;  // Raised arm
        this.model.rightArm.roll = -0.3f;   // Slightly outward
        
        // Left arm forward for balance
        this.model.leftArm.pitch = -0.5f;
        
        // Head looking forward aggressively (slight tilt down)
        this.model.head.pitch = 0.15f;
        this.model.hat.pitch = 0.15f;
        
        // Slight body lean forward for charging
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(5.0f));
        
        // Render the shadow clone with pure black translucent appearance
        // Use full brightness so the black silhouette stands out
        int fullBright = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        
        RenderLayer renderLayer = RenderLayer.getEntityTranslucent(SHADOW_TEXTURE);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);
        
        // Pure black with high opacity - creates a solid shadow appearance
        // ARGB format: Alpha=230 (0xE6), RGB=5,5,5 (very dark but not pure black for depth)
        int shadowColor = 0xE6050505;
        
        this.model.render(matrices, vertexConsumer, fullBright, OverlayTexture.DEFAULT_UV, shadowColor);
        
        // Render glowing eyes effect using emissive layer
        renderGlowingEyes(state, matrices, vertexConsumers);
        
        // Render the held weapon
        renderHeldItem(state, matrices, vertexConsumers, fullBright);
        
        matrices.pop();
        
        super.render(state, matrices, vertexConsumers, light);
    }
    
    /**
     * Resets all model parts to default pose.
     */
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
    
    /**
     * Renders glowing red eyes for the shadow clone.
     * Creates small red points on the face area.
     */
    private void renderGlowingEyes(ShadowCloneRenderState state, MatrixStack matrices, 
                                    VertexConsumerProvider vertexConsumers) {
        // Render an emissive layer for the eyes using the same texture
        // The eyes are painted red in the texture and will glow
        RenderLayer eyeLayer = RenderLayer.getEyes(SHADOW_TEXTURE);
        VertexConsumer eyeConsumer = vertexConsumers.getBuffer(eyeLayer);
        
        // Only render the head part with emissive eyes
        // Red glow color with full opacity
        int eyeColor = 0xFFFF2020;  // Bright red
        
        // Render just the head with glowing effect
        this.model.head.render(matrices, eyeConsumer, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, eyeColor);
    }
    
    /**
     * Renders the held weapon in the shadow clone's right hand.
     */
    private void renderHeldItem(ShadowCloneRenderState state, MatrixStack matrices,
                                VertexConsumerProvider vertexConsumers, int light) {
        ItemStack weapon = state.weaponStack;
        if (weapon.isEmpty()) {
            return;
        }
        
        matrices.push();
        
        // Transform to right arm position manually
        // Right arm pivot is at shoulder (5/16 from center, 22/16 up from pivot)
        matrices.translate(-0.3125f, 0.375f, 0.0f);  // Move to right shoulder area
        
        // Apply the arm's rotation (pitch for raised arm pose)
        float armPitch = this.model.rightArm.pitch;
        float armRoll = this.model.rightArm.roll;
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(armPitch * 57.2958f));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(armRoll * 57.2958f));
        
        // Translate down the arm to the hand
        matrices.translate(0.0f, 0.5625f, 0.0f);
        
        // Rotate item to be held properly in hand
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f));
        
        // Scale item
        matrices.scale(0.625f, 0.625f, 0.625f);
        
        // Render the item
        this.itemRenderer.renderItem(
            weapon,
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
}
