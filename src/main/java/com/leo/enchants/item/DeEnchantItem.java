package com.leo.enchants.item;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

/**
 * De-Enchant item - can only be held by players.
 * When used to hit any non-player target (blocks or entities),
 * creates a digital disintegration effect (1s and 0s fading away).
 * 
 * Attack handling is done via DeEnchantHandler event listener.
 */
public class DeEnchantItem extends Item {
    
    public DeEnchantItem(Settings settings) {
        super(settings);
    }
    
    /**
     * Add custom tooltip/lore to the item
     */
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, 
                              Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("Without colour, the world is made up of 0s and 1s")
                .formatted(Formatting.GRAY, Formatting.ITALIC));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
    
    /**
     * Make the item glow/shimmer in the inventory
     */
    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}
