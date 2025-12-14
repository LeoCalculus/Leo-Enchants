package com.leo.enchants.entity;

import com.leo.enchants.LeoEnchantsMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModEntities {
    
    public static final RegistryKey<EntityType<?>> GIANT_SWORD_KEY = RegistryKey.of(
        RegistryKeys.ENTITY_TYPE, 
        Identifier.of(LeoEnchantsMod.MOD_ID, "giant_sword")
    );
    
    public static final EntityType<GiantSwordEntity> GIANT_SWORD = Registry.register(
        Registries.ENTITY_TYPE,
        GIANT_SWORD_KEY,
        EntityType.Builder.<GiantSwordEntity>create(GiantSwordEntity::new, SpawnGroup.MISC)
            .dimensions(1.0f, 1.0f)
            .maxTrackingRange(256)
            .trackingTickInterval(1)
            .build(GIANT_SWORD_KEY)
    );
    
    public static void register() {
        LeoEnchantsMod.LOGGER.info("Registering Giant Sword Entity for " + LeoEnchantsMod.MOD_ID);
    }
}
