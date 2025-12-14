package com.leo.enchants.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "getAttributeValue", at = @At("RETURN"), cancellable = true)
    private void modifyAttackDamage(RegistryEntry<EntityAttribute> attribute, CallbackInfoReturnable<Double> cir) {
        if (!attribute.matches(EntityAttributes.ATTACK_DAMAGE)) {
            return;
        }

        LivingEntity entity = (LivingEntity) (Object) this;
        ItemStack mainHand = entity.getMainHandStack();
        
        if (mainHand.isEmpty()) {
            return;
        }

        // Check for Wither Impact enchantment
        ItemEnchantmentsComponent enchantments = mainHand.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null) {
            return;
        }

        Identifier witherImpactId = Identifier.of("leo_enchants", "wither_impact");
        int level = 0;
        
        for (var entry : enchantments.getEnchantmentEntries()) {
            if (entry.getKey().matchesId(witherImpactId)) {
                level = entry.getIntValue();
                break;
            }
        }

        if (level > 0) {
            double currentValue = cir.getReturnValue();
            double multiplier = 1.0 + (level * 0.2); // 1.2, 1.4, 1.6
            cir.setReturnValue(currentValue * multiplier);
        }
    }
}
