package com.leo.enchants.mixin;

import com.leo.enchants.accessor.MagnifyArrowAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    
    @Shadow
    public abstract EntityDimensions getDimensions(net.minecraft.entity.EntityPose pose);
    
    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void modifyDimensionsForMagnify(net.minecraft.entity.EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Entity self = (Entity) (Object) this;
        
        // Only handle magnified arrows here - player hitbox is handled in PlayerEntityMixin
        if (self instanceof PersistentProjectileEntity && self instanceof MagnifyArrowAccessor accessor) {
            float scale = accessor.leo_enchants$getMagnifyScale();
            if (scale > 1.0f) {
                EntityDimensions original = cir.getReturnValue();
                cir.setReturnValue(original.scaled(scale));
            }
        }
    }
}

