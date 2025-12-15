package com.leo.enchants;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.leo.enchants.entity.ModEntities;
import com.leo.enchants.item.ModItems;
import com.leo.enchants.logic.DeEnchantHandler;
import com.leo.enchants.logic.FallDamageImmunity;
import com.leo.enchants.logic.GiantSwordLogic;
import com.leo.enchants.logic.HookshotHandler;
import com.leo.enchants.logic.ObsidianLoreHandler;
import com.leo.enchants.logic.ShadowAssassinHandler;
import com.leo.enchants.logic.WitherImpactLogic;
import com.leo.enchants.network.ModNetworking;

public class LeoEnchantsMod implements ModInitializer {
    public static final String MOD_ID = "leo_enchants";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // In 1.21, Enchantments are data-driven and referenced by RegistryKey
    public static final RegistryKey<Enchantment> WITHER_IMPACT = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(MOD_ID, "wither_impact"));
    public static final RegistryKey<Enchantment> DOUBLE_JUMP = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(MOD_ID, "double_jump"));
    public static final RegistryKey<Enchantment> STRAFE = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(MOD_ID, "strafe"));
    public static final RegistryKey<Enchantment> MAGNIFY = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(MOD_ID, "magnify"));
    public static final RegistryKey<Enchantment> GRAB = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(MOD_ID, "grab"));
    public static final RegistryKey<Enchantment> GIANT = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(MOD_ID, "giant"));
    public static final RegistryKey<Enchantment> HOOKSHOT = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(MOD_ID, "hookshot"));
    public static final RegistryKey<Enchantment> SHADOW_ASSASSIN = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(MOD_ID, "shadow_assassin"));

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Leo Enchants...");

        // Note: In 1.21, we don't register the Enchantment class here. 
        // It must be defined in a JSON file at data/leo_enchants/enchantment/wither_impact.json
        // However, we can still reference it by key.

        // Register entities
        ModEntities.register();
        
        // Register items
        ModItems.register();
        
        // Register de-enchant item event handlers
        DeEnchantHandler.register();
        
        // Register obsidian lore item event handlers
        ObsidianLoreHandler.register();

        // Register networking for double jump fall damage immunity sync
        ModNetworking.registerServerReceivers();

        // Register server tick event for fall damage immunity (Wither Impact & Double Jump) and Hookshot holds
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long currentTime = server.getOverworld().getTime();
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                FallDamageImmunity.tickImmunity(player, currentTime);
            }
            
            // Tick hookshot holds in all worlds
            for (var world : server.getWorlds()) {
                HookshotHandler.tickHolds(world);
            }
            
            ShadowAssassinHandler.tickRestorations(server, currentTime);
        });

        // Register Event Listener for Wither Impact activation
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!stack.isEmpty() && stack.isIn(ItemTags.SWORDS)) {
                // Get the registry entry for the enchantment from the world's registry manager
                var registryOptional = world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT);
                
                if (registryOptional.isPresent()) {
                    var registry = registryOptional.get();
                    var enchantmentEntry = registry.getEntry(WITHER_IMPACT.getValue());

                    if (enchantmentEntry.isPresent()) {
                        int level = EnchantmentHelper.getLevel(enchantmentEntry.get(), stack);
                        if (level > 0) {
                            WitherImpactLogic.activate(player, world, level);
                            return ActionResult.SUCCESS;
                        }
                    }
                }
            }
            return ActionResult.PASS;
        });

        // Register Event Listener for Giant enchantment activation
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!stack.isEmpty() && stack.isIn(ItemTags.SWORDS)) {
                // Get the registry entry for the Giant enchantment from the world's registry manager
                var registryOptional = world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT);
                
                if (registryOptional.isPresent()) {
                    var registry = registryOptional.get();
                    var giantEntry = registry.getEntry(GIANT.getValue());

                    if (giantEntry.isPresent()) {
                        int level = EnchantmentHelper.getLevel(giantEntry.get(), stack);
                        if (level > 0) {
                            // Check if player is sneaking to activate Giant (to differentiate from Wither Impact)
                            if (player.isSneaking()) {
                                boolean success = GiantSwordLogic.activate(player, world, level);
                                if (success) {
                                    return ActionResult.SUCCESS;
                                }
                            }
                        }
                    }
                }
            }
            return ActionResult.PASS;
        });

        // Register Event Listener for Shadow Assassin chestplate activation
        UseItemCallback.EVENT.register(ShadowAssassinHandler::tryActivate);
    }
}
