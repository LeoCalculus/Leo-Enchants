package com.leo.enchants.accessor;

/**
 * Interface to track missile mode on creepers for rendering.
 */
public interface MissileCreeperAccessor {
    
    void leo_enchants$setMissileMode(boolean active);
    
    boolean leo_enchants$isMissileMode();
    
    void leo_enchants$setMissilePitch(float pitch);
    
    float leo_enchants$getMissilePitch();
}




