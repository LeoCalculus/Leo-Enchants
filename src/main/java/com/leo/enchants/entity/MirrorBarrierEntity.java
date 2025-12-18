package com.leo.enchants.entity;

import com.leo.enchants.item.ModItems;
import com.leo.enchants.logic.MirrorWorldHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * Entity representing a floating Mirror Barrier item at the center of a mirror world.
 * This entity floats in mid-air and provides visual indication of the mirror world's center.
 */
public class MirrorBarrierEntity extends Entity {
    
    private ItemStack displayedItem = ItemStack.EMPTY;
    private UUID mirrorWorldId = null;
    private int rotationTicks = 0;
    private float floatOffset = 0;
    
    public MirrorBarrierEntity(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;  // Don't collide with blocks
    }
    
    public MirrorBarrierEntity(World world, Vec3d pos, ItemStack item, UUID mirrorWorldId) {
        this(ModEntities.MIRROR_BARRIER, world);
        this.setPosition(pos);
        this.displayedItem = item.copy();
        this.mirrorWorldId = mirrorWorldId;
    }
    
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        // No data tracked for now
    }
    
    @Override
    public void tick() {
        super.tick();
        
        rotationTicks++;
        floatOffset = (float) Math.sin(rotationTicks * 0.05) * 0.1f;
        
        // Server-side logic
        if (!getWorld().isClient()) {
            // Check if the mirror world still exists
            if (mirrorWorldId != null && !MirrorWorldHandler.mirrorWorldExists(mirrorWorldId)) {
                this.discard();
                return;
            }
            
            // Server-side particle spawning
            if (rotationTicks % 10 == 0 && getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.END_ROD,
                    getX(), getY() + 0.5 + floatOffset, getZ(),
                    3, 0.3, 0.3, 0.3, 0.01);
            }
        }
    }
    
    @Override
    public void readCustomData(ReadView readView) {
        // Reconstruct the Mirror Barrier item for display
        this.displayedItem = new ItemStack(ModItems.MIRROR_BARRIER);
        
        String uuidStr = readView.getString("MirrorWorldId", "");
        if (!uuidStr.isEmpty()) {
            try {
                this.mirrorWorldId = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException ignored) {}
        }
        
        this.rotationTicks = readView.getInt("RotationTicks", 0);
    }
    
    @Override
    public void writeCustomData(WriteView writeView) {
        if (!displayedItem.isEmpty()) {
            // Store item ID for reference
            writeView.putString("DisplayedItemId", displayedItem.getItem().toString());
        }
        if (mirrorWorldId != null) {
            writeView.putString("MirrorWorldId", mirrorWorldId.toString());
        }
        writeView.putInt("RotationTicks", rotationTicks);
    }
    
    public ItemStack getDisplayedItem() {
        return displayedItem;
    }
    
    public void setDisplayedItem(ItemStack item) {
        this.displayedItem = item.copy();
    }
    
    public UUID getMirrorWorldId() {
        return mirrorWorldId;
    }
    
    public void setMirrorWorldId(UUID id) {
        this.mirrorWorldId = id;
    }
    
    public int getRotationTicks() {
        return rotationTicks;
    }
    
    public float getFloatOffset() {
        return floatOffset;
    }
    
    @Override
    public boolean shouldRender(double distance) {
        return distance < 256 * 256;  // Render from far away
    }
    
    @Override
    public boolean isAttackable() {
        return false;
    }
    
    @Override
    public boolean canHit() {
        return false;
    }
    
    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        return false; // Cannot be damaged
    }
}
