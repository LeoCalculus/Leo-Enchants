package com.leo.enchants.client;

import com.leo.enchants.entity.HerobrineObsidianEntity;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * Renders Herobrine's floating obsidian projectiles as rotating obsidian blocks.
 */
public class HerobrineObsidianRenderer extends EntityRenderer<HerobrineObsidianEntity, HerobrineObsidianRenderState> {

    private final BlockRenderManager blockRenderManager;

    public HerobrineObsidianRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.5f;
        this.blockRenderManager = context.getBlockRenderManager();
    }

    @Override
    public HerobrineObsidianRenderState createRenderState() {
        return new HerobrineObsidianRenderState();
    }

    @Override
    public void updateRenderState(HerobrineObsidianEntity entity, HerobrineObsidianRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.state = entity.getState();
        state.delay = entity.getDelay();
        state.tickDelta = tickDelta;
        state.age = entity.age;
    }

    @Override
    public void render(HerobrineObsidianRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        // Don't render if still in delay
        if (state.delay > 0) {
            return;
        }
        
        matrices.push();
        
        // Center the block
        matrices.translate(-0.5, -0.5, -0.5);
        
        // Scale based on state
        float scale = 0.8f;
        if (state.state == 1) { // Charging
            // Pulsing scale during charge
            float pulse = MathHelper.sin((state.age + state.tickDelta) * 0.5f) * 0.1f;
            scale = 0.8f + pulse;
        } else if (state.state == 2) { // Flying
            scale = 0.9f;
        }
        
        matrices.translate(0.5, 0.5, 0.5);
        matrices.scale(scale, scale, scale);
        
        // Rotation animation
        float rotationSpeed = switch (state.state) {
            case 0 -> 1.0f; // Hovering - slow rotation
            case 1 -> 3.0f; // Charging - faster rotation
            case 2 -> 5.0f; // Flying - fastest rotation
            default -> 1.0f;
        };
        
        float rotation = (state.age + state.tickDelta) * rotationSpeed;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotation * 0.7f));
        
        matrices.translate(-0.5, -0.5, -0.5);
        
        // Render obsidian block
        var blockState = Blocks.OBSIDIAN.getDefaultState();
        
        blockRenderManager.renderBlockAsEntity(
            blockState,
            matrices,
            vertexConsumers,
            light,
            OverlayTexture.DEFAULT_UV
        );
        
        matrices.pop();
        
        super.render(state, matrices, vertexConsumers, light);
    }
}

