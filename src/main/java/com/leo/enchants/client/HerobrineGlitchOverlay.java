package com.leo.enchants.client;

import com.leo.enchants.entity.HerobrineEntity;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

public class HerobrineGlitchOverlay implements HudRenderCallback {
    private static final Random RANDOM = new Random();
    private static int glitchTicks = 0;

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        if (client.options.getPerspective().isFrontView()) return;

        // Check for Herobrine in Phase 3 nearby
        boolean shouldShowGlitch = false;
        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof HerobrineEntity herobrine && herobrine.getPhase() == 3) {
                if (client.player.distanceTo(herobrine) < 40) {
                    shouldShowGlitch = true;
                    break;
                }
            }
        }

        if (!shouldShowGlitch) return;

        glitchTicks++;
        int width = drawContext.getScaledWindowWidth();
        int height = drawContext.getScaledWindowHeight();

        // 1. Draw subtle red vignette/flash
        if (glitchTicks % 20 < 5) {
            drawContext.fill(0, 0, width, height, 0x33FF0000);
        }

        // 2. Draw glitchy "ERROR" text at random positions
        if (glitchTicks % 5 == 0) {
            int errorCount = 3 + RANDOM.nextInt(5);
            for (int i = 0; i < errorCount; i++) {
                int x = RANDOM.nextInt(width - 50);
                int y = RANDOM.nextInt(height - 20);
                float scale = 1.0f + RANDOM.nextFloat() * 2.0f;
                
                drawContext.getMatrices().pushMatrix();
                drawContext.getMatrices().translate(x, y);
                drawContext.getMatrices().scale(scale, scale);
                
                String text = RANDOM.nextBoolean() ? "ERROR" : "SYSTEM FAILURE";
                int color = RANDOM.nextBoolean() ? 0xFFFF0000 : 0xFF770000;
                
                drawContext.drawText(client.textRenderer, Text.literal(text).formatted(Formatting.BOLD), 0, 0, color, false);
                drawContext.getMatrices().popMatrix();
            }
        }

        // 3. Static noise effect (small dots)
        for (int i = 0; i < 20; i++) {
            int x = RANDOM.nextInt(width);
            int y = RANDOM.nextInt(height);
            drawContext.fill(x, y, x + 1, y + 1, 0x55FF0000);
        }
        
        // 4. Horizontal glitch lines
        if (glitchTicks % 10 < 3) {
            int lineY = RANDOM.nextInt(height);
            int lineHeight = 1 + RANDOM.nextInt(3);
            drawContext.fill(0, lineY, width, lineY + lineHeight, 0x44FF0000);
        }
    }
}

