package com.leo.enchants.item;

import com.leo.enchants.logic.MirrorWorldHandler;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.function.Consumer;

/**
 * Mirror Barrier - Creates a mirror dimension that duplicates reality.
 * 
 * When activated:
 * - Opens a mirror world covering 50 block radius
 * - The item floats in mid-air at the activation point (consumed on use)
 * - All changes in the mirror world don't affect the real world
 * - After 1 minute, the mirror cancels and reveals the real world
 * - Maximum 5 mirror worlds can exist simultaneously in a dimension
 * - Prevents item duplication by tracking inventories
 */
public class MirrorBarrierItem extends Item {
    
    public MirrorBarrierItem(Settings settings) {
        super(settings);
    }
    
    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        
        ItemStack stack = player.getStackInHand(hand);
        ServerWorld serverWorld = (ServerWorld) world;
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        
        // Check if we can create a new mirror world (max 5)
        if (!MirrorWorldHandler.canCreateMirrorWorld(serverWorld)) {
            player.sendMessage(Text.literal("§c[Mirror Barrier] §7Maximum mirror worlds reached (5). Wait for one to expire."), true);
            return ActionResult.FAIL;
        }
        
        // Check if player is already in a mirror world
        if (MirrorWorldHandler.isInMirrorWorld(serverPlayer)) {
            player.sendMessage(Text.literal("§c[Mirror Barrier] §7Cannot create a mirror world inside another mirror world!"), true);
            return ActionResult.FAIL;
        }
        
        // IMPORTANT: Consume the item FIRST, before snapshot is taken
        // This ensures the consumed item won't be in the inventory snapshot
        ItemStack itemForDisplay = stack.copy();
        stack.decrement(1);
        
        // Now activate the mirror world (snapshot will NOT include the consumed item)
        boolean success = MirrorWorldHandler.createMirrorWorld(serverWorld, serverPlayer, itemForDisplay);
        
        if (success) {
            player.sendMessage(Text.literal("§b[Mirror Barrier] §7Mirror world activated! Duration: §e60 seconds"), true);
            return ActionResult.SUCCESS;
        } else {
            // Failed - give the item back
            player.getInventory().insertStack(itemForDisplay);
            player.sendMessage(Text.literal("§c[Mirror Barrier] §7Failed to create mirror world."), true);
            return ActionResult.FAIL;
        }
    }
    
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("A fragment of dimensional glass...")
                .formatted(Formatting.AQUA, Formatting.ITALIC));
        textConsumer.accept(Text.literal(""));
        textConsumer.accept(Text.literal("Right-click to activate (consumed)").formatted(Formatting.YELLOW));
        textConsumer.accept(Text.literal("• Creates a 50 block radius mirror world").formatted(Formatting.GRAY));
        textConsumer.accept(Text.literal("• Changes in mirror don't affect reality").formatted(Formatting.GRAY));
        textConsumer.accept(Text.literal("• Lasts for 60 seconds").formatted(Formatting.GRAY));
        textConsumer.accept(Text.literal("• Max 5 mirror worlds per dimension").formatted(Formatting.GRAY));
        textConsumer.accept(Text.literal(""));
        textConsumer.accept(Text.literal("§c⚠ Consumed on use - not returned").formatted(Formatting.RED));
        
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
    
    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}
