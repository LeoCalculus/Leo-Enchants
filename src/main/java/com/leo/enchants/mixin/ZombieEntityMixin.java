package com.leo.enchants.mixin;

import com.leo.enchants.monster.ZombiePlaceBlockGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to enhance zombie AI with block-placing abilities.
 * Zombies can now place blocks to reach players when blocked by gaps, lava, or height differences.
 */
@Mixin(ZombieEntity.class)
public abstract class ZombieEntityMixin extends HostileEntity {

    protected ZombieEntityMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    /**
     * Inject our custom block-placing goal into the zombie's AI.
     * We inject at the end of initGoals() to add our goal after vanilla goals are set up.
     */
    @Inject(method = "initGoals", at = @At("TAIL"))
    private void addBlockPlacingGoal(CallbackInfo ci) {
        ZombieEntity zombie = (ZombieEntity) (Object) this;
        
        // Priority 2 - higher priority than melee attack (priority 2) but lower than swimming (priority 0)
        // This ensures zombies will try to place blocks when they can't reach their target
        this.goalSelector.add(1, new ZombiePlaceBlockGoal(zombie));
    }
}
