package com.velorise.veinfarming;

import com.velorise.veinfarming.client.ClientEventHandler;
import com.velorise.veinfarming.network.FarmingKeyPayload;
import com.velorise.veinfarming.network.ToggleSmartPlantPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.SimpleChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(VeinFarmingMod.MODID)
public class VeinFarmingMod {
    public static final String MODID = "vein_farming";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(MODID, "main"))
            .networkProtocolVersion(1)
            .optional()
            .simpleChannel();

    public VeinFarmingMod(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, FarmingConfig.SPEC);
        registerPayloads();

        MinecraftForge.EVENT_BUS.register(new FarmingEventHandler());

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientEventHandler.initClient(context);
        }

        preloadClasses();

        LOGGER.info("Vein Farming: Universal Crop Harvester (Forge 1.21.1) initialized successfully!");
    }

    private static void preloadClasses() {
        Class<?>[] classes = new Class<?>[]{
                FarmingConfig.class,
                PlantClassifier.class,
                PlantingAlgorithms.class,
                HarvestManager.class,
                PlantingManager.class,
                TillingAndFertilizingManager.class,
                FarmingManager.class,
                ModAdvancements.class,
                LiteMinerCompat.class,
                FTBUltimineCompat.class,
                FarmingEventHandler.class
        };
        for (Class<?> clazz : classes) {
            try {
                Class.forName(clazz.getName(), true, VeinFarmingMod.class.getClassLoader());
            } catch (Throwable t) {
                LOGGER.warn("Vein Farming: Preloading class {} encountered note: {}", clazz.getName(), t.getMessage());
            }
        }
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
