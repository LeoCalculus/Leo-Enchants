package com.leo.enchants.item;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

/**
 * Lore of Obsidian - An enchanted obsidian that cannot be placed.
 * Inspired by MrFudgeMonkey's animation of Herobrine controlling obsidian.
 * 
 * Has two modes:
 * - Build Mode (default): Right-click two positions to create an obsidian bridge
 * - Attack Mode: Shift+right-click to switch; right-click to launch obsidian strikes
 * 
 * The enchantment "Obsidian Mastery" can only be obtained through crafting 9 obsidians.
 */
public class ObsidianLoreItem extends Item {
    
    // NBT keys for storing item state
    public static final String MODE_KEY = "ObsidianMode";
    public static final String FIRST_POS_X = "FirstPosX";
    public static final String FIRST_POS_Y = "FirstPosY";
    public static final String FIRST_POS_Z = "FirstPosZ";
    public static final String HAS_FIRST_POS = "HasFirstPos";
    
    // Mode constants
    public static final int BUILD_MODE = 0;
    public static final int ATTACK_MODE = 1;
    
    public ObsidianLoreItem(Settings settings) {
        super(settings);
    }
    
    /**
     * Prevent placing as a block - this enchanted obsidian cannot be placed
     */
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        // We handle this in the logic handler - return PASS to allow it to be processed there
        return ActionResult.PASS;
    }
    
    /**
     * Get the current mode from the item stack
     */
    public static int getMode(ItemStack stack) {
        NbtCompound nbt = stack.getOrDefault(net.minecraft.component.DataComponentTypes.CUSTOM_DATA, 
            net.minecraft.component.type.NbtComponent.DEFAULT).copyNbt();
        return nbt.getInt(MODE_KEY).orElse(BUILD_MODE);
    }
    
    /**
     * Set the mode on the item stack
     */
    public static void setMode(ItemStack stack, int mode) {
        NbtCompound nbt = stack.getOrDefault(net.minecraft.component.DataComponentTypes.CUSTOM_DATA,
            net.minecraft.component.type.NbtComponent.DEFAULT).copyNbt();
        nbt.putInt(MODE_KEY, mode);
        stack.set(net.minecraft.component.DataComponentTypes.CUSTOM_DATA, 
            net.minecraft.component.type.NbtComponent.of(nbt));
    }
    
    /**
     * Toggle between build and attack mode
     */
    public static void toggleMode(ItemStack stack, PlayerEntity player) {
        int currentMode = getMode(stack);
        int newMode = (currentMode == BUILD_MODE) ? ATTACK_MODE : BUILD_MODE;
        setMode(stack, newMode);
        
        // Clear any stored first position when switching modes
        clearFirstPosition(stack);
        
        String modeName = (newMode == BUILD_MODE) ? "Build Mode" : "Attack Mode";
        player.sendMessage(Text.literal("§5[Obsidian Mastery] §7Switched to §d" + modeName), true);
    }
    
    /**
     * Store the first position for bridge building
     */
    public static void setFirstPosition(ItemStack stack, double x, double y, double z) {
        NbtCompound nbt = stack.getOrDefault(net.minecraft.component.DataComponentTypes.CUSTOM_DATA,
            net.minecraft.component.type.NbtComponent.DEFAULT).copyNbt();
        nbt.putDouble(FIRST_POS_X, x);
        nbt.putDouble(FIRST_POS_Y, y);
        nbt.putDouble(FIRST_POS_Z, z);
        nbt.putBoolean(HAS_FIRST_POS, true);
        stack.set(net.minecraft.component.DataComponentTypes.CUSTOM_DATA,
            net.minecraft.component.type.NbtComponent.of(nbt));
    }
    
    /**
     * Check if a first position is stored
     */
    public static boolean hasFirstPosition(ItemStack stack) {
        NbtCompound nbt = stack.getOrDefault(net.minecraft.component.DataComponentTypes.CUSTOM_DATA,
            net.minecraft.component.type.NbtComponent.DEFAULT).copyNbt();
        return nbt.getBoolean(HAS_FIRST_POS).orElse(false);
    }
    
    /**
     * Get the stored first position
     */
    public static double[] getFirstPosition(ItemStack stack) {
        NbtCompound nbt = stack.getOrDefault(net.minecraft.component.DataComponentTypes.CUSTOM_DATA,
            net.minecraft.component.type.NbtComponent.DEFAULT).copyNbt();
        return new double[] {
            nbt.getDouble(FIRST_POS_X).orElse(0.0),
            nbt.getDouble(FIRST_POS_Y).orElse(0.0),
            nbt.getDouble(FIRST_POS_Z).orElse(0.0)
        };
    }
    
    /**
     * Clear the stored first position
     */
    public static void clearFirstPosition(ItemStack stack) {
        NbtCompound nbt = stack.getOrDefault(net.minecraft.component.DataComponentTypes.CUSTOM_DATA,
            net.minecraft.component.type.NbtComponent.DEFAULT).copyNbt();
        nbt.remove(FIRST_POS_X);
        nbt.remove(FIRST_POS_Y);
        nbt.remove(FIRST_POS_Z);
        nbt.putBoolean(HAS_FIRST_POS, false);
        stack.set(net.minecraft.component.DataComponentTypes.CUSTOM_DATA,
            net.minecraft.component.type.NbtComponent.of(nbt));
    }
    
    /**
     * Add custom tooltip showing current mode and instructions
     */
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        int mode = getMode(stack);
        
        textConsumer.accept(Text.literal("The power of Herobrine flows within...")
                .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC));
        textConsumer.accept(Text.literal(""));
        
        if (mode == BUILD_MODE) {
            textConsumer.accept(Text.literal("Mode: ").formatted(Formatting.GRAY)
                    .append(Text.literal("Build").formatted(Formatting.AQUA)));
            textConsumer.accept(Text.literal("• Right-click to set bridge points").formatted(Formatting.DARK_GRAY));
            textConsumer.accept(Text.literal("• Max distance: 50 blocks").formatted(Formatting.DARK_GRAY));
        } else {
            textConsumer.accept(Text.literal("Mode: ").formatted(Formatting.GRAY)
                    .append(Text.literal("Attack").formatted(Formatting.RED)));
            textConsumer.accept(Text.literal("• Right-click to launch obsidian strike").formatted(Formatting.DARK_GRAY));
            textConsumer.accept(Text.literal("• Damage scales with distance").formatted(Formatting.DARK_GRAY));
        }
        
        textConsumer.accept(Text.literal(""));
        textConsumer.accept(Text.literal("Shift + Right-click to switch modes").formatted(Formatting.YELLOW));
        
        if (hasFirstPosition(stack)) {
            double[] pos = getFirstPosition(stack);
            textConsumer.accept(Text.literal(String.format("First point: %.1f, %.1f, %.1f", pos[0], pos[1], pos[2]))
                    .formatted(Formatting.GREEN));
        }
        
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
