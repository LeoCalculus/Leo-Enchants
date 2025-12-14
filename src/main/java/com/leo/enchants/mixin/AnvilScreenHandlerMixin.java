package com.leo.enchants.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin {

    private static Field levelCostField;
    private static Field inputField;

    static {
        try {
            // Try to find the levelCost field using various possible names
            levelCostField = findField(AnvilScreenHandler.class, "levelCost", "field_7770", "field_22480");
            if (levelCostField != null) {
                levelCostField.setAccessible(true);
            }
            
            // Find the input field
            inputField = findField(AnvilScreenHandler.class, "input", "field_7767");
            if (inputField != null) {
                inputField.setAccessible(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Field findField(Class<?> clazz, String... names) {
        for (String name : names) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        // Try superclass
        if (clazz.getSuperclass() != null) {
            return findField(clazz.getSuperclass(), names);
        }
        return null;
    }

    @Inject(method = "updateResult", at = @At("TAIL"))
    private void onUpdateResult(CallbackInfo ci) {
        if (levelCostField == null || inputField == null) {
            return;
        }

        try {
            AnvilScreenHandler handler = (AnvilScreenHandler) (Object) this;
            
            // Get the input inventory using reflection
            Object inputInventory = inputField.get(handler);
            if (inputInventory == null) return;
            
            // Get input items using the inventory's getStack method
            ItemStack input1 = ((net.minecraft.inventory.Inventory) inputInventory).getStack(0);
            ItemStack input2 = ((net.minecraft.inventory.Inventory) inputInventory).getStack(1);

            if (input1.isEmpty() || input2.isEmpty()) {
                return;
            }

            Identifier witherImpactId = Identifier.of("leo_enchants", "wither_impact");
            
            int level1 = getWitherImpactLevel(input1, witherImpactId);
            int level2 = getWitherImpactLevel(input2, witherImpactId);

            if (level1 > 0 && level2 > 0 && level1 == level2) {
                Property levelCost = (Property) levelCostField.get(handler);
                
                if (level1 == 1) {
                    levelCost.set(30);
                } else if (level1 == 2) {
                    levelCost.set(60);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getWitherImpactLevel(ItemStack stack, Identifier enchantId) {
        ItemEnchantmentsComponent enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null) return 0;
        
        for (var entry : enchantments.getEnchantmentEntries()) {
            if (entry.getKey().matchesId(enchantId)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }
}
