package com.leo.enchants.mixin.client;

/**
 * Interface to access magnify scale from render state.
 */
public interface MagnifyRenderStateAccessor {
    float leo_enchants$getMagnifyScale();
    void leo_enchants$setMagnifyScale(float scale);
}

