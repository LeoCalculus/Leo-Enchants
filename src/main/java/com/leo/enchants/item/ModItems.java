package com.leo.enchants.item;

import com.leo.enchants.LeoEnchantsMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public class ModItems {
    
    public static final RegistryKey<Item> DE_ENCHANT_KEY = RegistryKey.of(
        RegistryKeys.ITEM,
        Identifier.of(LeoEnchantsMod.MOD_ID, "de_enchant")
    );
    
    public static final RegistryKey<Item> OBSIDIAN_LORE_KEY = RegistryKey.of(
        RegistryKeys.ITEM,
        Identifier.of(LeoEnchantsMod.MOD_ID, "obsidian_lore")
    );
    
    public static final RegistryKey<Item> MIRROR_BARRIER_KEY = RegistryKey.of(
        RegistryKeys.ITEM,
        Identifier.of(LeoEnchantsMod.MOD_ID, "mirror_barrier")
    );
    
    public static final Item DE_ENCHANT = Registry.register(
        Registries.ITEM,
        DE_ENCHANT_KEY,
        new DeEnchantItem(new Item.Settings()
            .maxCount(64)
            .rarity(Rarity.EPIC)
            .registryKey(DE_ENCHANT_KEY))
    );
    
    public static final Item OBSIDIAN_LORE = Registry.register(
        Registries.ITEM,
        OBSIDIAN_LORE_KEY,
        new ObsidianLoreItem(new Item.Settings()
            .maxCount(1)
            .rarity(Rarity.EPIC)
            .registryKey(OBSIDIAN_LORE_KEY))
    );
    
    public static final Item MIRROR_BARRIER = Registry.register(
        Registries.ITEM,
        MIRROR_BARRIER_KEY,
        new MirrorBarrierItem(new Item.Settings()
            .maxCount(1)
            .rarity(Rarity.EPIC)
            .registryKey(MIRROR_BARRIER_KEY))
    );
    
    public static void register() {
        LeoEnchantsMod.LOGGER.info("Registering De-Enchant Item for " + LeoEnchantsMod.MOD_ID);
        LeoEnchantsMod.LOGGER.info("Registering Obsidian Lore Item for " + LeoEnchantsMod.MOD_ID);
        LeoEnchantsMod.LOGGER.info("Registering Mirror Barrier Item for " + LeoEnchantsMod.MOD_ID);
        
        // Add item to the Tools creative tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(DE_ENCHANT);
            entries.add(OBSIDIAN_LORE);
            entries.add(MIRROR_BARRIER);
        });
        
        // Also add to Operator Utilities (since it uses command blocks)
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.OPERATOR).register(entries -> {
            entries.add(DE_ENCHANT);
            entries.add(OBSIDIAN_LORE);
            entries.add(MIRROR_BARRIER);
        });
    }
}

