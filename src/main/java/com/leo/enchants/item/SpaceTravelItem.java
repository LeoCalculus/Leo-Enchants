package com.leo.enchants.item;

import com.leo.enchants.entity.SpaceTravelPortalEntity;
import com.leo.enchants.logic.SpaceTravelHandler;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.function.Consumer;

/**
 * Space Travel - Opens a curved dimensional portal surface in front of the player.
 * 
 * Features:
 * - Not stackable, consumed on use
 * - Crafted with dirt + obsidian + end_stone (shapeless)
 * - Right-click opens a curved portal surface in mid-air
 * - Right-shift cycles through dimensions (Overworld, Nether, End)
 * - Portal surface shows a preview of the destination dimension
 * - When player enters, portal closes after 3 seconds in both worlds
 */
public class SpaceTravelItem extends Item {
    
    public SpaceTravelItem(Settings settings) {
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
        
        // Check if player is sneaking to cycle dimensions
        if (player.isSneaking()) {
            SpaceTravelHandler.cycleDimension(serverPlayer);
            return ActionResult.SUCCESS;
        }
        
        // Check if player already has an active portal
        if (SpaceTravelHandler.hasActivePortal(serverPlayer)) {
            player.sendMessage(Text.literal("§c[Space Travel] §7You already have an active portal!"), true);
            return ActionResult.FAIL;
        }
        
        // Calculate portal position (3 blocks in front of player, at eye level)
        Vec3d lookVec = player.getRotationVec(1.0f);
        Vec3d portalPos = player.getEyePos().add(lookVec.multiply(3.0));
        
        // Get the target dimension
        String targetDimension = SpaceTravelHandler.getSelectedDimension(serverPlayer);
        
        // Create the portal entity
        SpaceTravelPortalEntity portal = new SpaceTravelPortalEntity(
            serverWorld, 
            portalPos, 
            serverPlayer.getUuid(),
            targetDimension,
            player.getYaw(),
            player.getPitch()
        );
        
        serverWorld.spawnEntity(portal);
        
        // Register the portal
        SpaceTravelHandler.registerPortal(serverPlayer, portal);
        
        // Consume the item
        stack.decrement(1);
        
        String dimensionName = SpaceTravelHandler.getDimensionDisplayName(targetDimension);
        player.sendMessage(Text.literal("§b[Space Travel] §7Portal opened to §e" + dimensionName + "§7! Walk through to travel."), true);
        
        return ActionResult.SUCCESS;
    }
    
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.literal("A fragment of spacetime itself...")
                .formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC));
        textConsumer.accept(Text.literal(""));
        textConsumer.accept(Text.literal("§6Right-click §7to open dimensional portal").formatted(Formatting.YELLOW));
        textConsumer.accept(Text.literal("§6Shift + Right-click §7to cycle dimensions").formatted(Formatting.YELLOW));
        textConsumer.accept(Text.literal(""));
        textConsumer.accept(Text.literal("• Opens curved portal surface in mid-air").formatted(Formatting.GRAY));
        textConsumer.accept(Text.literal("• Preview destination through the portal").formatted(Formatting.GRAY));
        textConsumer.accept(Text.literal("• Portal closes 3s after you enter").formatted(Formatting.GRAY));
        textConsumer.accept(Text.literal(""));
        textConsumer.accept(Text.literal("Destinations: Overworld, Nether, End").formatted(Formatting.DARK_PURPLE));
        textConsumer.accept(Text.literal(""));
        textConsumer.accept(Text.literal("§c⚠ Consumed on use").formatted(Formatting.RED));
        
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
    
    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}


