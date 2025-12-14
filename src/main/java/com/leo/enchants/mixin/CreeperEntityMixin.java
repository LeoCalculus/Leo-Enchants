package com.leo.enchants.mixin;

import com.leo.enchants.monster.CreeperMissileGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to enhance creepers with missile mode.
 * 50% of creepers can become missiles that launch toward players and explode on arrival.
 */
@Mixin(CreeperEntity.class)
public abstract class CreeperEntityMixin extends HostileEntity {

    @Unique
    private boolean leo_enchants$canBeMissile = false;
    
    @Unique
    private boolean leo_enchants$missileRolled = false;

    protected CreeperEntityMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    /**
     * Add the missile goal to creepers.
     * The goal itself handles the 50% chance logic.
     */
    @Inject(method = "initGoals", at = @At("TAIL"))
    private void addMissileGoal(CallbackInfo ci) {
        CreeperEntity creeper = (CreeperEntity) (Object) this;
        
        // Add missile goal with high priority (lower number = higher priority)
        // Priority 1 so it takes precedence over normal creeper behavior when active
        this.goalSelector.add(1, new CreeperMissileGoal(creeper));
    }
}
