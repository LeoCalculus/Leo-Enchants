package com.leo.enchants.accessor;

/**
 * Interface injected into PersistentProjectileEntity to track heavy arrows from enhanced skeletons.
 * Heavy arrows deal 2x damage.
 */
public interface HeavyArrowAccessor {
    
    void leo_enchants$setHeavyArrow(boolean heavy);
    
    boolean leo_enchants$isHeavyArrow();
}

