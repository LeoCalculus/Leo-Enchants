package com.leo.enchants.entity;

import com.leo.enchants.LeoEnchantsMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

public class ModEntities {
    
    public static final RegistryKey<EntityType<?>> GIANT_SWORD_KEY = RegistryKey.of(
        RegistryKeys.ENTITY_TYPE, 
        Identifier.of(LeoEnchantsMod.MOD_ID, "giant_sword")
    );
    public static final RegistryKey<EntityType<?>> SHADOW_CLONE_KEY = RegistryKey.of(
        RegistryKeys.ENTITY_TYPE,
        Identifier.of(LeoEnchantsMod.MOD_ID, "shadow_clone")
    );
    public static final RegistryKey<EntityType<?>> DIGIT_DISINTEGRATION_KEY = RegistryKey.of(
        RegistryKeys.ENTITY_TYPE,
        Identifier.of(LeoEnchantsMod.MOD_ID, "digit_disintegration")
    );
    public static final RegistryKey<EntityType<?>> OBSIDIAN_BRIDGE_KEY = RegistryKey.of(
        RegistryKeys.ENTITY_TYPE,
        Identifier.of(LeoEnchantsMod.MOD_ID, "obsidian_bridge")
    );
    public static final RegistryKey<EntityType<?>> OBSIDIAN_STRIKE_KEY = RegistryKey.of(
        RegistryKeys.ENTITY_TYPE,
        Identifier.of(LeoEnchantsMod.MOD_ID, "obsidian_strike")
    );
    public static final RegistryKey<EntityType<?>> MIRROR_BARRIER_KEY = RegistryKey.of(
        RegistryKeys.ENTITY_TYPE,
        Identifier.of(LeoEnchantsMod.MOD_ID, "mirror_barrier")
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
    
    public static final EntityType<ShadowCloneEntity> SHADOW_CLONE = Registry.register(
        Registries.ENTITY_TYPE,
        SHADOW_CLONE_KEY,
        EntityType.Builder.<ShadowCloneEntity>create(ShadowCloneEntity::new, SpawnGroup.MISC)
            .dimensions(0.6f, 1.8f)
            .maxTrackingRange(64)
            .trackingTickInterval(1)
            .build(SHADOW_CLONE_KEY)
    );
    
    public static final EntityType<DigitDisintegrationEntity> DIGIT_DISINTEGRATION = Registry.register(
        Registries.ENTITY_TYPE,
        DIGIT_DISINTEGRATION_KEY,
        EntityType.Builder.<DigitDisintegrationEntity>create(DigitDisintegrationEntity::new, SpawnGroup.MISC)
            .dimensions(0.1f, 0.1f)
            .maxTrackingRange(64)
            .trackingTickInterval(2)
            .build(DIGIT_DISINTEGRATION_KEY)
    );
    
    public static final EntityType<ObsidianBridgeEntity> OBSIDIAN_BRIDGE = Registry.register(
        Registries.ENTITY_TYPE,
        OBSIDIAN_BRIDGE_KEY,
        EntityType.Builder.<ObsidianBridgeEntity>create(ObsidianBridgeEntity::new, SpawnGroup.MISC)
            .dimensions(0.5f, 0.5f)
            .maxTrackingRange(128)
            .trackingTickInterval(1)
            .build(OBSIDIAN_BRIDGE_KEY)
    );
    
    public static final EntityType<ObsidianStrikeEntity> OBSIDIAN_STRIKE = Registry.register(
        Registries.ENTITY_TYPE,
        OBSIDIAN_STRIKE_KEY,
        EntityType.Builder.<ObsidianStrikeEntity>create(ObsidianStrikeEntity::new, SpawnGroup.MISC)
            .dimensions(0.5f, 0.5f)
            .maxTrackingRange(128)
            .trackingTickInterval(1)
            .build(OBSIDIAN_STRIKE_KEY)
    );
    
    public static final EntityType<MirrorBarrierEntity> MIRROR_BARRIER = Registry.register(
        Registries.ENTITY_TYPE,
        MIRROR_BARRIER_KEY,
        EntityType.Builder.<MirrorBarrierEntity>create(MirrorBarrierEntity::new, SpawnGroup.MISC)
            .dimensions(0.5f, 0.5f)
            .maxTrackingRange(128)
            .trackingTickInterval(1)
            .build(MIRROR_BARRIER_KEY)
    );
    
    public static void register() {
        LeoEnchantsMod.LOGGER.info("Registering Giant Sword Entity for " + LeoEnchantsMod.MOD_ID);
        // Register shadow clone with proper living entity attributes
        FabricDefaultAttributeRegistry.register(SHADOW_CLONE, ShadowCloneEntity.createShadowCloneAttributes());
        LeoEnchantsMod.LOGGER.info("Registering Shadow Clone Entity for " + LeoEnchantsMod.MOD_ID);
        LeoEnchantsMod.LOGGER.info("Registering Digit Disintegration Entity for " + LeoEnchantsMod.MOD_ID);
        LeoEnchantsMod.LOGGER.info("Registering Obsidian Bridge Entity for " + LeoEnchantsMod.MOD_ID);
        LeoEnchantsMod.LOGGER.info("Registering Obsidian Strike Entity for " + LeoEnchantsMod.MOD_ID);
        LeoEnchantsMod.LOGGER.info("Registering Mirror Barrier Entity for " + LeoEnchantsMod.MOD_ID);
    }
}
