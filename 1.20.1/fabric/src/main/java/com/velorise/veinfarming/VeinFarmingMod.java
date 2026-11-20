package com.velorise.veinfarming;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VeinFarmingMod implements ModInitializer {
    public static final String MODID = "vein_farming";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public static final ResourceLocation KEY_STATE_PACKET = new ResourceLocation(MODID, "key_state");
    public static final ResourceLocation TOGGLE_SMART_PLANT_PACKET = new ResourceLocation(MODID, "toggle_smart_plant");

    @Override
    public void onInitialize() {
        FarmingConfig.load();

        ServerPlayNetworking.registerGlobalReceiver(KEY_STATE_PACKET, (server, player, handler, buf, responseSender) -> {
            boolean active = buf.readBoolean();
            server.execute(() -> FarmingEventHandler.setPlayerKeyActive(player.getUUID(), active));
        });

        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_SMART_PLANT_PACKET, (server, player, handler, buf, responseSender) -> {
            boolean enabled = buf.readBoolean();
            server.execute(() -> FarmingEventHandler.setPlayerSmartPlantEnabled(player.getUUID(), enabled));
        });

        UseBlockCallback.EVENT.register(FarmingEventHandler::onRightClickBlock);
        PlayerBlockBreakEvents.BEFORE.register(FarmingEventHandler::onBlockBreak);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                FarmingEventHandler.onPlayerLoggedOut(handler.getPlayer().getUUID()));

        LOGGER.info("Vein Farming: Universal Crop Harvester (Fabric 1.20.1) initialized successfully!");
    }
}
