package com.leo.enchants.accessor;

/**
 * Interface injected into PersistentProjectileEntity to track magnify level and scale.
 */
public interface MagnifyArrowAccessor {
    
    void leo_enchants$setMagnifyLevel(int level);
    
    int leo_enchants$getMagnifyLevel();
    
    float leo_enchants$getMagnifyScale();
}



