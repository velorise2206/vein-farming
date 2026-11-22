package com.velorise.veinfarming;

import com.velorise.veinfarming.client.ClientEventHandler;
import com.velorise.veinfarming.network.FarmingKeyPayload;
import com.velorise.veinfarming.network.ToggleSmartPlantPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.SimpleChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

@Mod(VeinFarmingMod.MODID)
public class VeinFarmingMod {
    public static final String MODID = "vein_farming";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(Identifier.fromNamespaceAndPath(MODID, "main"))
            .networkProtocolVersion(1)
            .optional()
            .simpleChannel();

    public VeinFarmingMod(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, FarmingConfig.SPEC);
        registerPayloads();

        BusGroup.DEFAULT.register(MethodHandles.lookup(), new FarmingEventHandler());

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientEventHandler.initClient(context);
        }

        LOGGER.info("Vein Farming (Forge 26.3) initialized!");
    }

    private static void registerPayloads() {
        int id = 0;
        CHANNEL.messageBuilder(FarmingKeyPayload.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder((payload, buf) -> FarmingKeyPayload.STREAM_CODEC.encode(buf, payload))
                .decoder(FarmingKeyPayload.STREAM_CODEC::decode)
                .consumerMainThread((payload, ctx) -> {
                    ServerPlayer sp = ctx.getSender();
                    if (sp != null) {
                        FarmingEventHandler.setPlayerKeyActive(sp.getUUID(), payload.active());
                    }
                })
                .add();

        CHANNEL.messageBuilder(ToggleSmartPlantPayload.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder((payload, buf) -> ToggleSmartPlantPayload.STREAM_CODEC.encode(buf, payload))
                .decoder(ToggleSmartPlantPayload.STREAM_CODEC::decode)
                .consumerMainThread((payload, ctx) -> {
                    ServerPlayer sp = ctx.getSender();
                    if (sp != null) {
                        FarmingEventHandler.setPlayerSmartPlantEnabled(sp.getUUID(), payload.enabled());
                    }
                })
                .add();
    }
}
