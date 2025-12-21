package com.leo.enchants.accessor;

import java.util.UUID;

/**
 * Interface injected into PersistentProjectileEntity to track enhanced skeleton arrows.
 * - Heavy arrows deal 3x damage (considering Power enchantment)
 * - Tracking arrows home in on the target player
 */
public interface HeavyArrowAccessor {
    
    void leo_enchants$setHeavyArrow(boolean heavy);
    
    boolean leo_enchants$isHeavyArrow();
    
    void leo_enchants$setTrackingArrow(boolean tracking);
    
    boolean leo_enchants$isTrackingArrow();
    
    void leo_enchants$setTrackingTarget(UUID targetUuid);
    
    UUID leo_enchants$getTrackingTarget();
}


