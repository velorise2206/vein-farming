package com.velorise.veinfarming;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

public final class PlatformBridge {
    private static final TagKey<Item> C_HOES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "tools/hoes"));

    private PlatformBridge() {}

    public static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    public static boolean isDirectWaterBucket(ItemStack stack) {
        return stack.is(Items.WATER_BUCKET) || stack.is(PlantClassifier.C_WATER_BUCKETS);
    }

    public static boolean hasFluidWater(ItemStack stack) {
        try {
            var storage = FluidStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack));
            if (storage != null) {
                for (var view : storage) {
                    if (view.getResource().isOf(Fluids.WATER) && view.getAmount() >= FluidConstants.BUCKET) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static int getWaterBuckets(ItemStack stack) {
        if (isDirectWaterBucket(stack)) {
            return stack.getCount();
        }
        int count = 0;
        try {
            var storage = FluidStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack));
            if (storage != null) {
                for (var view : storage) {
                    if (view.getResource().isOf(Fluids.WATER)) {
                        count += (int) (view.getAmount() / FluidConstants.BUCKET);
                    }
                }
            }
        } catch (Throwable ignored) {}
        return count;
    }

    public static boolean drainOneFluidWaterOffhand(Player player, ItemStack stack) {
        if (isDirectWaterBucket(stack)) return false;
        try {
            var context = ContainerItemContext.ofPlayerHand(player, InteractionHand.OFF_HAND);
            var storage = FluidStorage.ITEM.find(stack, context);
            if (storage != null) {
                try (Transaction tx = Transaction.openOuter()) {
                    long extracted = storage.extract(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, tx);
                    if (extracted >= FluidConstants.BUCKET) {
                        tx.commit();
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static boolean drainOneFluidWaterSlot(Player player, int slotIndex, ItemStack stack) {
        if (isDirectWaterBucket(stack)) return false;
        try {
            var slot = PlayerInventoryStorage.of(player).getSlot(slotIndex);
            var context = ContainerItemContext.ofPlayerSlot(player, slot);
            var storage = FluidStorage.ITEM.find(stack, context);
            if (storage != null) {
                try (Transaction tx = Transaction.openOuter()) {
                    long extracted = storage.extract(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, tx);
                    if (extracted >= FluidConstants.BUCKET) {
                        tx.commit();
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static boolean isHoe(ItemStack stack) {
        return stack.is(ItemTags.HOES) || stack.is(C_HOES);
    }

    public static BlockState getTilledState(Level level, Player player, InteractionHand hand, BlockPos pos, BlockState state) {
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.DIRT_PATH)) {
            return Blocks.FARMLAND.defaultBlockState();
        }
        if (state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)) {
            return Blocks.DIRT.defaultBlockState();
        }
        return null;
    }

    public static boolean applyBoneMeal(ItemStack stack, Level level, BlockPos pos, Player player) {
        return BoneMealItem.growCrop(stack, level, pos);
    }

    public static SoundType getSoundType(BlockState state, Level level, BlockPos pos, Entity entity) {
        return state.getSoundType();
    }
}
