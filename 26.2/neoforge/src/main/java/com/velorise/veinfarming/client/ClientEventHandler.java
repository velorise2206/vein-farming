package com.velorise.veinfarming.client;

import com.velorise.veinfarming.VeinFarmingMod;
import com.velorise.veinfarming.network.FarmingKeyPayload;
import com.velorise.veinfarming.network.ToggleSmartPlantPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@EventBusSubscriber(modid = VeinFarmingMod.MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    private static boolean lastKeyActive = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            lastKeyActive = false;
            return;
        }

        while (ModKeyMappings.KEY_TOGGLE_SMART_PLANT.consumeClick()) {
            boolean newState = !ModKeyMappings.isSmartPlantEnabled();
            ModKeyMappings.setSmartPlantEnabled(newState);
            ClientPacketDistributor.sendToServer(new ToggleSmartPlantPayload(newState));

            Component msg = newState
                    ? Component.translatable("message.vein_farming.smart_plant.enabled").withStyle(ChatFormatting.GREEN)
                    : Component.translatable("message.vein_farming.smart_plant.disabled").withStyle(ChatFormatting.RED);
            mc.player.sendOverlayMessage(msg);
        }

        boolean currentActive = ModKeyMappings.isKeyActive();
        if (currentActive != lastKeyActive) {
            lastKeyActive = currentActive;
            ClientPacketDistributor.sendToServer(new FarmingKeyPayload(currentActive));
        }
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(ModKeyMappings.CATEGORY);
        event.register(ModKeyMappings.KEY_ACTIVATE);
        event.register(ModKeyMappings.KEY_TOGGLE_SMART_PLANT);
    }
}
