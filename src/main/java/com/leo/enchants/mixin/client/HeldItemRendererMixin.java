package com.leo.enchants.mixin.client;

import com.leo.enchants.item.ModItems;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to hide the Obsidian Lore item when held in the offhand.
 * The item model is too large and blocks the screen when in offhand.
 */
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void leo_enchants$hideObsidianLoreInOffhand(
            AbstractClientPlayerEntity player,
            float tickDelta,
            float pitch,
            Hand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci) {
        
        // Don't render Obsidian Lore in offhand
        if (hand == Hand.OFF_HAND && item.isOf(ModItems.OBSIDIAN_LORE)) {
            ci.cancel();
        }
    }
}

