package com.leo.enchants.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(BowItem.class)
public class BowItemMixin {

    @Unique
    private static final Identifier MAGNIFY_ID = Identifier.of("leo_enchants", "magnify");

    @Inject(
        method = "onStoppedUsing",
        at = @At("RETURN")
    )
    private void applyMagnifyEffects(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> cir) {
        if (world.isClient()) {
            return;
        }
        
        int magnifyLevel = getMagnifyLevel(stack);
        if (magnifyLevel <= 0) {
            return;
        }
        
        // Find recently spawned arrows near the user with larger radius
        List<PersistentProjectileEntity> nearbyArrows = world.getEntitiesByClass(
            PersistentProjectileEntity.class,
            user.getBoundingBox().expand(5.0),
            arrow -> arrow.getOwner() == user && arrow.age <= 5
        );
        
        for (PersistentProjectileEntity arrow : nearbyArrows) {
            if (arrow instanceof MagnifyArrowAccessor accessor) {
                // Only apply if not already set
                if (accessor.leo_enchants$getMagnifyLevel() == 0) {
                    accessor.leo_enchants$setMagnifyLevel(magnifyLevel);
                }
            }
        }
        
        // Apply extra durability damage (10, 20, 30 for levels 1, 2, 3) minus the 1 already consumed
        if (world instanceof ServerWorld serverWorld) {
            int extraDurability = magnifyLevel * 10 - 1;
            if (extraDurability > 0) {
                stack.damage(extraDurability, serverWorld, 
                    user instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer ? serverPlayer : null, 
                    (item) -> {});
            }
        }
    }

    @Unique
    private int getMagnifyLevel(ItemStack stack) {
        ItemEnchantmentsComponent enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null) {
            return 0;
        }

        for (var entry : enchantments.getEnchantmentEntries()) {
            if (entry.getKey().matchesId(MAGNIFY_ID)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }
}
