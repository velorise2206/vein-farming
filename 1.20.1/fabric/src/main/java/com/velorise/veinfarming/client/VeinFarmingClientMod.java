package com.velorise.veinfarming.client;

import com.velorise.veinfarming.VeinFarmingMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

public class VeinFarmingClientMod implements ClientModInitializer {

    private static boolean lastKeyActive = false;
    private static final StandaloneHighlightRenderer HIGHLIGHT_RENDERER = new StandaloneHighlightRenderer();

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(ModKeyMappings.KEY_ACTIVATE);
        KeyBindingHelper.registerKeyBinding(ModKeyMappings.KEY_TOGGLE_SMART_PLANT);

        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register(HIGHLIGHT_RENDERER::handleBeforeBlockOutline);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(HIGHLIGHT_RENDERER::handleRenderWorld);

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) {
                lastKeyActive = false;
                return;
            }

            while (ModKeyMappings.KEY_TOGGLE_SMART_PLANT.consumeClick()) {
                boolean newState = !ModKeyMappings.isSmartPlantEnabled();
                ModKeyMappings.setSmartPlantEnabled(newState);
                if (ClientPlayNetworking.canSend(VeinFarmingMod.TOGGLE_SMART_PLANT_PACKET)) {
                    FriendlyByteBuf buf = PacketByteBufs.create();
                    buf.writeBoolean(newState);
                    ClientPlayNetworking.send(VeinFarmingMod.TOGGLE_SMART_PLANT_PACKET, buf);
                }

                Component msg = newState
                        ? Component.translatable("message.vein_farming.smart_plant.enabled").withStyle(ChatFormatting.GREEN)
                        : Component.translatable("message.vein_farming.smart_plant.disabled").withStyle(ChatFormatting.RED);
                mc.player.displayClientMessage(msg, true);
            }

            boolean currentActive = ModKeyMappings.isKeyActive();
            if (currentActive != lastKeyActive) {
                lastKeyActive = currentActive;
                if (ClientPlayNetworking.canSend(VeinFarmingMod.KEY_STATE_PACKET)) {
                    FriendlyByteBuf buf = PacketByteBufs.create();
                    buf.writeBoolean(currentActive);
                    ClientPlayNetworking.send(VeinFarmingMod.KEY_STATE_PACKET, buf);
                }
            }
        });
    }
}
