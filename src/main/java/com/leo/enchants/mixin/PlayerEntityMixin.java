package com.leo.enchants.mixin;

import com.leo.enchants.accessor.PlayerHitboxAccessor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements PlayerHitboxAccessor {
    
    @Unique
    private float leo_enchants$hitboxScale = 1.0f;

    @Override
    public void leo_enchants$setHitboxScale(float scale) {
        this.leo_enchants$hitboxScale = scale;
    }

    @Override
    public float leo_enchants$getHitboxScale() {
        return this.leo_enchants$hitboxScale;
    }
    
    /**
     * Inject at the end of tick to resize bounding box if scale is greater than 1
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void leo_enchants$resizeBoundingBoxOnTick(CallbackInfo ci) {
        if (this.leo_enchants$hitboxScale > 1.0f) {
            PlayerEntity self = (PlayerEntity)(Object)this;
            Box originalBox = self.getBoundingBox();
            
            // Calculate center of bounding box
            double centerX = (originalBox.minX + originalBox.maxX) / 2.0;
            double centerZ = (originalBox.minZ + originalBox.maxZ) / 2.0;
            double minY = originalBox.minY;
            
            // Original player dimensions (approximately 0.6 width, 1.8 height)
            double originalWidth = 0.6;
            double originalHeight = 1.8;
            
            // Calculate new dimensions
            double newHalfWidth = (originalWidth * this.leo_enchants$hitboxScale) / 2.0;
            double newHeight = originalHeight * this.leo_enchants$hitboxScale;
            
            // Create expanded bounding box centered on player
            Box expandedBox = new Box(
                centerX - newHalfWidth,
                minY,
                centerZ - newHalfWidth,
                centerX + newHalfWidth,
                minY + newHeight,
                centerZ + newHalfWidth
            );
            
            self.setBoundingBox(expandedBox);
        }
    }
}

