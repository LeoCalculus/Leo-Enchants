package com.leo.enchants.accessor.client;

/**
 * Interface for accessing missile mode data from creeper render state.
 */
public interface MissileCreeperRenderStateAccessor {
    
    void leo_enchants$setMissileMode(boolean active);
    
    boolean leo_enchants$isMissileMode();
    
    void leo_enchants$setMissilePitch(float pitch);
    
    float leo_enchants$getMissilePitch();
}

