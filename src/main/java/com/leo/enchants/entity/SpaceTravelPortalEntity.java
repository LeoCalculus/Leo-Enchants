package com.leo.enchants.entity;

import com.leo.enchants.logic.SpaceTravelHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

/**
 * Entity representing the curved dimensional portal surface.
 * The portal displays a preview of the destination dimension and 
 * teleports players who walk through it.
 */
public class SpaceTravelPortalEntity extends Entity {
    
    private static final TrackedData<String> TARGET_DIMENSION = DataTracker.registerData(
        SpaceTravelPortalEntity.class, TrackedDataHandlerRegistry.STRING
    );
    private static final TrackedData<Float> PORTAL_YAW = DataTracker.registerData(
        SpaceTravelPortalEntity.class, TrackedDataHandlerRegistry.FLOAT
    );
    private static final TrackedData<Integer> CLOSING_TICKS = DataTracker.registerData(
        SpaceTravelPortalEntity.class, TrackedDataHandlerRegistry.INTEGER
    );
    private static final TrackedData<Boolean> IS_CLOSING = DataTracker.registerData(
        SpaceTravelPortalEntity.class, TrackedDataHandlerRegistry.BOOLEAN
    );
    
    private UUID ownerUuid = null;
    private int lifetimeTicks = 0;
    private float portalPitch = 0;
    private static final int MAX_LIFETIME = 20 * 60; // 60 seconds max
    private static final int CLOSING_DURATION = 20 * 3; // 3 seconds
    
    // Portal dimensions for the curved surface
    public static final float PORTAL_WIDTH = 3.0f;
    public static final float PORTAL_HEIGHT = 3.0f;
    public static final float PORTAL_CURVE_DEPTH = 1.0f;
    
    public SpaceTravelPortalEntity(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
    }
    
    public SpaceTravelPortalEntity(World world, Vec3d pos, UUID ownerUuid, String targetDimension, float yaw, float pitch) {
        this(ModEntities.SPACE_TRAVEL_PORTAL, world);
        this.setPosition(pos);
        this.ownerUuid = ownerUuid;
        this.dataTracker.set(TARGET_DIMENSION, targetDimension);
        this.dataTracker.set(PORTAL_YAW, yaw);
        this.portalPitch = pitch;
    }
    
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(TARGET_DIMENSION, "minecraft:overworld");
        builder.add(PORTAL_YAW, 0.0f);
        builder.add(CLOSING_TICKS, 0);
        builder.add(IS_CLOSING, false);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        lifetimeTicks++;
        
        if (!getWorld().isClient()) {
            ServerWorld serverWorld = (ServerWorld) getWorld();
            
            // Check if portal has exceeded max lifetime
            if (lifetimeTicks > MAX_LIFETIME) {
                closePortal();
            }
            
            // Handle closing animation
            if (dataTracker.get(IS_CLOSING)) {
                int closingTicks = dataTracker.get(CLOSING_TICKS) + 1;
                dataTracker.set(CLOSING_TICKS, closingTicks);
                
                if (closingTicks >= CLOSING_DURATION) {
                    // Unregister portal and remove
                    if (ownerUuid != null) {
                        SpaceTravelHandler.unregisterPortal(ownerUuid);
                    }
                    this.discard();
                    return;
                }
            }
            
            // Check for players entering the portal
            if (!dataTracker.get(IS_CLOSING)) {
                checkForPlayerEntry(serverWorld);
            }
            
            // Spawn particles
            if (lifetimeTicks % 2 == 0) {
                spawnPortalParticles(serverWorld);
            }
        }
    }
    
    private void checkForPlayerEntry(ServerWorld world) {
        // Create a hitbox for the curved portal surface
        Box portalBox = new Box(
            getX() - PORTAL_WIDTH / 2, getY() - PORTAL_HEIGHT / 2, getZ() - PORTAL_CURVE_DEPTH,
            getX() + PORTAL_WIDTH / 2, getY() + PORTAL_HEIGHT / 2, getZ() + PORTAL_CURVE_DEPTH
        );
        
        List<PlayerEntity> playersInRange = world.getEntitiesByClass(
            PlayerEntity.class, 
            portalBox, 
            player -> player instanceof ServerPlayerEntity
        );
        
        for (PlayerEntity player : playersInRange) {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                // Teleport the player
                SpaceTravelHandler.teleportThroughPortal(serverPlayer, this);
                
                // Start closing the portal
                startClosing();
                break;
            }
        }
    }
    
    private void spawnPortalParticles(ServerWorld world) {
        float yaw = (float) Math.toRadians(dataTracker.get(PORTAL_YAW));
        
        // Spawn particles along the curved edge of the portal
        for (int i = 0; i < 5; i++) {
            double angle = (Math.random() - 0.5) * Math.PI; // -90 to 90 degrees for the curve
            double offsetX = Math.cos(angle) * PORTAL_CURVE_DEPTH;
            double offsetZ = Math.sin(angle) * (PORTAL_WIDTH / 2);
            double offsetY = (Math.random() - 0.5) * PORTAL_HEIGHT;
            
            // Rotate based on portal yaw
            double rotatedX = offsetX * Math.cos(yaw) - offsetZ * Math.sin(yaw);
            double rotatedZ = offsetX * Math.sin(yaw) + offsetZ * Math.cos(yaw);
            
            world.spawnParticles(
                ParticleTypes.PORTAL,
                getX() + rotatedX, getY() + offsetY, getZ() + rotatedZ,
                1, 0.1, 0.1, 0.1, 0.02
            );
        }
        
        // Add dimension-specific particles
        String dimension = dataTracker.get(TARGET_DIMENSION);
        if (dimension.contains("nether")) {
            world.spawnParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 2, 0.5, 0.5, 0.5, 0.01);
        } else if (dimension.contains("end")) {
            world.spawnParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 2, 0.5, 0.5, 0.5, 0.01);
        } else {
            world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY(), getZ(), 1, 0.5, 0.5, 0.5, 0.01);
        }
    }
    
    public void startClosing() {
        dataTracker.set(IS_CLOSING, true);
        dataTracker.set(CLOSING_TICKS, 0);
    }
    
    public void closePortal() {
        if (!dataTracker.get(IS_CLOSING)) {
            startClosing();
        }
    }
    
    @Override
    public void readCustomData(ReadView readView) {
        String uuidStr = readView.getString("OwnerUuid", "");
        if (!uuidStr.isEmpty()) {
            try {
                this.ownerUuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException ignored) {}
        }
        
        this.lifetimeTicks = readView.getInt("LifetimeTicks", 0);
        this.portalPitch = readView.getFloat("PortalPitch", 0);
        this.dataTracker.set(TARGET_DIMENSION, readView.getString("TargetDimension", "minecraft:overworld"));
        this.dataTracker.set(PORTAL_YAW, readView.getFloat("PortalYaw", 0));
        this.dataTracker.set(CLOSING_TICKS, readView.getInt("ClosingTicks", 0));
        this.dataTracker.set(IS_CLOSING, readView.getBoolean("IsClosing", false));
    }
    
    @Override
    public void writeCustomData(WriteView writeView) {
        if (ownerUuid != null) {
            writeView.putString("OwnerUuid", ownerUuid.toString());
        }
        writeView.putInt("LifetimeTicks", lifetimeTicks);
        writeView.putFloat("PortalPitch", portalPitch);
        writeView.putString("TargetDimension", dataTracker.get(TARGET_DIMENSION));
        writeView.putFloat("PortalYaw", dataTracker.get(PORTAL_YAW));
        writeView.putInt("ClosingTicks", dataTracker.get(CLOSING_TICKS));
        writeView.putBoolean("IsClosing", dataTracker.get(IS_CLOSING));
    }
    
    public String getTargetDimension() {
        return dataTracker.get(TARGET_DIMENSION);
    }
    
    public float getPortalYaw() {
        return dataTracker.get(PORTAL_YAW);
    }
    
    public float getPortalPitch() {
        return portalPitch;
    }
    
    public int getClosingTicks() {
        return dataTracker.get(CLOSING_TICKS);
    }
    
    public boolean isClosing() {
        return dataTracker.get(IS_CLOSING);
    }
    
    public int getLifetimeTicks() {
        return lifetimeTicks;
    }
    
    public UUID getOwnerUuid() {
        return ownerUuid;
    }
    
    @Override
    public boolean shouldRender(double distance) {
        return distance < 256 * 256;
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
        return false;
    }
}

