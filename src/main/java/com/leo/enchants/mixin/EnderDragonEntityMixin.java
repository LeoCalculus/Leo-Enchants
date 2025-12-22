package com.leo.enchants.mixin;

import com.leo.enchants.LeoEnchantsMod;
import com.leo.enchants.entity.HerobrineEntity;
import com.leo.enchants.entity.ModEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to spawn Herobrine when the Ender Dragon is killed.
 */
@Mixin(EnderDragonEntity.class)
public abstract class EnderDragonEntityMixin extends MobEntity {

    protected EnderDragonEntityMixin(EntityType<? extends MobEntity> entityType, World world) {
        super(entityType, world);
    }

    /**
     * Inject at the end of updatePostDeath to spawn Herobrine after dragon death animation.
     */
    @Inject(method = "updatePostDeath", at = @At("TAIL"))
    private void onDragonDeath(CallbackInfo ci) {
        EnderDragonEntity dragon = (EnderDragonEntity) (Object) this;
        
        // Check if dragon is about to be removed (death animation complete)
        if (dragon.deathTime == 200) { // Dragon removes itself at deathTime 200
            World world = dragon.getWorld();
            
            if (!world.isClient && world instanceof ServerWorld serverWorld) {
                // Spawn Herobrine at the end fountain (0, 64, 0 approximately)
                BlockPos spawnPos = new BlockPos(0, 75, 0);
                
                // Check if Herobrine is already present (don't spawn multiple)
                boolean herobrineExists = serverWorld.getEntitiesByType(
                    ModEntities.HEROBRINE,
                    dragon.getBoundingBox().expand(200),
                    e -> true
                ).size() > 0;
                
                if (!herobrineExists) {
                    spawnHerobrine(serverWorld, spawnPos);
                }
            }
        }
    }
    
    private void spawnHerobrine(ServerWorld world, BlockPos pos) {
        LeoEnchantsMod.LOGGER.info("Spawning Herobrine after Ender Dragon death!");
        
        // Dramatic pre-spawn effects
        // Lightning strikes
        for (int i = 0; i < 5; i++) {
            double angle = (2 * Math.PI / 5) * i;
            double radius = 10;
            double x = pos.getX() + Math.cos(angle) * radius;
            double z = pos.getZ() + Math.sin(angle) * radius;
            
            world.spawnParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                x, pos.getY(), z,
                50, 0.5, 2, 0.5, 0.1
            );
        }
        
        // Spawn sound
        world.playSound(null, pos, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 3.0f, 0.5f);
        world.playSound(null, pos, SoundEvents.AMBIENT_CAVE.value(), SoundCategory.HOSTILE, 2.0f, 0.3f);
        
        // Create Herobrine
        HerobrineEntity herobrine = ModEntities.HEROBRINE.create(world, SpawnReason.TRIGGERED);
        if (herobrine != null) {
            herobrine.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            world.spawnEntity(herobrine);
            
            // Dramatic spawn particles
            for (int i = 0; i < 100; i++) {
                world.spawnParticles(
                    ParticleTypes.LARGE_SMOKE,
                    pos.getX() + 0.5 + (world.random.nextDouble() - 0.5) * 3,
                    pos.getY() + world.random.nextDouble() * 3,
                    pos.getZ() + 0.5 + (world.random.nextDouble() - 0.5) * 3,
                    1, 0, 0.1, 0, 0.05
                );
            }
            
            // Broadcast message to all players
            world.getPlayers().forEach(player -> {
                player.sendMessage(Text.literal("§4§l⚠ Herobrine has awakened! ⚠"), false);
            });
            
            LeoEnchantsMod.LOGGER.info("Herobrine spawned at {}", pos);
        }
    }
}


