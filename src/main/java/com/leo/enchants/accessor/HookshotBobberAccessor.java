package com.leo.enchants.accessor;

import net.minecraft.util.math.BlockPos;

/**
 * Accessor interface for hookshot bobber functionality.
 * Implemented by FishingBobberEntityMixin.
 */
public interface HookshotBobberAccessor {
    
    void leo_enchants$setHookshotBobber(boolean isHookshot);
    
    boolean leo_enchants$isHookshotBobber();
    
    void leo_enchants$setAttachedBlock(BlockPos pos);
    
    BlockPos leo_enchants$getAttachedBlock();
}




