package com.leo.enchants.network;

import com.leo.enchants.LeoEnchantsMod;
import com.leo.enchants.logic.FallDamageImmunity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ModNetworking {
    
    // Packet ID for double jump fall damage immunity request
    public static final Identifier DOUBLE_JUMP_IMMUNITY_ID = Identifier.of(LeoEnchantsMod.MOD_ID, "double_jump_immunity");
    
    /**
     * Payload for requesting fall damage immunity after double jump
     */
    public record DoubleJumpImmunityPayload() implements CustomPayload {
        public static final Id<DoubleJumpImmunityPayload> ID = new Id<>(DOUBLE_JUMP_IMMUNITY_ID);
        public static final PacketCodec<RegistryByteBuf, DoubleJumpImmunityPayload> CODEC = PacketCodec.unit(new DoubleJumpImmunityPayload());
        
        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
    
    /**
     * Register server-side packet handlers (call from main mod initializer)
     */
    public static void registerServerReceivers() {
        // Register the payload type
        PayloadTypeRegistry.playC2S().register(DoubleJumpImmunityPayload.ID, DoubleJumpImmunityPayload.CODEC);
        
        // Handle double jump immunity request from client
        ServerPlayNetworking.registerGlobalReceiver(DoubleJumpImmunityPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            // Grant fall damage immunity on the server
            context.server().execute(() -> {
                long currentTime = context.server().getOverworld().getTime();
                FallDamageImmunity.grantImmunity(player, currentTime);
                LeoEnchantsMod.LOGGER.debug("Granted double jump fall damage immunity to {}", player.getName().getString());
            });
        });
    }
    
    /**
     * Register client-side packet senders (call from client mod initializer)
     */
    public static void registerClientSenders() {
        // Register the payload type on client side too
        PayloadTypeRegistry.playC2S().register(DoubleJumpImmunityPayload.ID, DoubleJumpImmunityPayload.CODEC);
    }
    
    /**
     * Send double jump immunity request to server (call from client when double jump is triggered)
     */
    public static void sendDoubleJumpImmunityRequest() {
        if (ClientPlayNetworking.canSend(DoubleJumpImmunityPayload.ID)) {
            ClientPlayNetworking.send(new DoubleJumpImmunityPayload());
        }
    }
}

