package com.velorise.veinfarming.client;

import com.velorise.veinfarming.VeinFarmingMod;
import com.velorise.veinfarming.network.FarmingKeyPayload;
import com.velorise.veinfarming.network.ToggleSmartPlantPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = VeinFarmingMod.MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    private static boolean lastKeyActive = false;

    public static void initClient(FMLJavaModLoadingContext context) {
        MinecraftForge.EVENT_BUS.register(new StandaloneHighlightRenderer());
        context.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (mc, screen) -> new FarmingConfigScreen(screen)
                )
        );
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
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
            mc.player.displayClientMessage(msg, true);
        }

        boolean currentActive = ModKeyMappings.isKeyActive();
        if (currentActive != lastKeyActive) {
            lastKeyActive = currentActive;
            VeinFarmingMod.CHANNEL.send(new FarmingKeyPayload(currentActive), PacketDistributor.SERVER.noArg());
        }
    }

    @Mod.EventBusSubscriber(modid = VeinFarmingMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(ModKeyMappings.KEY_ACTIVATE);
            event.register(ModKeyMappings.KEY_TOGGLE_SMART_PLANT);
        }
    }
}
