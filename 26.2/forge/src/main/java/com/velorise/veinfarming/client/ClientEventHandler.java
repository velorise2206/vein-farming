package com.velorise.veinfarming.client;

import com.velorise.veinfarming.VeinFarmingMod;
import com.velorise.veinfarming.network.FarmingKeyPayload;
import com.velorise.veinfarming.network.ToggleSmartPlantPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.PacketDistributor;

public class ClientEventHandler {

    private static boolean lastKeyActive = false;

    public static void initClient(FMLJavaModLoadingContext context) {
        context.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (mc, screen) -> new FarmingConfigScreen(screen)
                )
        );
        RegisterKeyMappingsEvent.BUS.addListener(ClientEventHandler::registerKeyMappings);
        TickEvent.ClientTickEvent.Post.BUS.addListener(ClientEventHandler::onClientTick);
        RenderHighlightEvent.Block.BUS.addListener(StandaloneHighlightRenderer::onExtractBlockOutline);
    }

    private static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            lastKeyActive = false;
            return;
        }

        while (ModKeyMappings.KEY_TOGGLE_SMART_PLANT.consumeClick()) {
            boolean newState = !ModKeyMappings.isSmartPlantEnabled();
            ModKeyMappings.setSmartPlantEnabled(newState);
            VeinFarmingMod.CHANNEL.send(new ToggleSmartPlantPayload(newState), PacketDistributor.SERVER.noArg());

            Component msg = newState
                    ? Component.translatable("message.vein_farming.smart_plant.enabled").withStyle(ChatFormatting.GREEN)
                    : Component.translatable("message.vein_farming.smart_plant.disabled").withStyle(ChatFormatting.RED);
            mc.player.sendOverlayMessage(msg);
        }

        boolean currentActive = ModKeyMappings.isKeyActive();
        if (currentActive != lastKeyActive) {
            lastKeyActive = currentActive;
            VeinFarmingMod.CHANNEL.send(new FarmingKeyPayload(currentActive), PacketDistributor.SERVER.noArg());
        }
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.KEY_ACTIVATE);
        event.register(ModKeyMappings.KEY_TOGGLE_SMART_PLANT);
    }
}
