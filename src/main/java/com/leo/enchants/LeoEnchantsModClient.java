package com.leo.enchants;

import com.leo.enchants.client.DigitDisintegrationRenderer;
import com.leo.enchants.client.GiantSwordEntityRenderer;
import com.leo.enchants.client.ShadowCloneRenderer;
import com.leo.enchants.entity.ModEntities;
import com.leo.enchants.logic.DoubleJumpHandler;
import com.leo.enchants.logic.StrafeHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class LeoEnchantsModClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        LeoEnchantsMod.LOGGER.info("Initializing Leo Enchants Client...");
        
        // Register entity renderers
        EntityRendererRegistry.register(ModEntities.GIANT_SWORD, GiantSwordEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.SHADOW_CLONE, ShadowCloneRenderer::new);
        EntityRendererRegistry.register(ModEntities.DIGIT_DISINTEGRATION, DigitDisintegrationRenderer::new);
        
        // Register client tick events for enchantment handling
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            DoubleJumpHandler.tick(client);
            StrafeHandler.tick(client);
        });
    }
}

