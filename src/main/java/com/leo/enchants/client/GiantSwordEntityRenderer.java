package com.leo.enchants.client;

import com.leo.enchants.entity.GiantSwordEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class GiantSwordEntityRenderer extends EntityRenderer<GiantSwordEntity, GiantSwordRenderState> {
    
    private final ItemRenderer itemRenderer;
    
    public GiantSwordEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = MinecraftClient.getInstance().getItemRenderer();
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
        state.swordStack = createSwordStack(entity.getSwordType());
    }
    
    @Override
    public void render(GiantSwordRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        
        ItemStack swordStack = state.swordStack.isEmpty() ? createSwordStack(state.swordType) : state.swordStack;
        
        // Keep the sword enormous like the old custom model
        float scale = state.swordSize * 0.6f;
        
        // Point downward and correct vanilla item diagonal tilt
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(225));
        // matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-45));
        
        matrices.scale(scale, scale, scale);
        
        itemRenderer.renderItem(
            swordStack,
            ItemDisplayContext.NONE,
            light,
            OverlayTexture.DEFAULT_UV,
            matrices,
            vertexConsumers,
            null,
            0
        );
        
        matrices.pop();
    }
    
    private ItemStack createSwordStack(String swordType) {
        Identifier id = Identifier.tryParse(swordType);
        if (id != null && Registries.ITEM.containsId(id)) {
            return new ItemStack(Registries.ITEM.get(id));
        }
        return new ItemStack(Items.DIAMOND_SWORD);
    }
}
