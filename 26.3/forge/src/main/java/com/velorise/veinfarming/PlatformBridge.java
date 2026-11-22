package com.velorise.veinfarming;

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
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.ModList;

public final class PlatformBridge {
    private static final TagKey<Item> C_HOES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "tools/hoes"));

    private PlatformBridge() {}

    public static boolean isModLoaded(String modId) {
        return ModList.isLoaded(modId);
    }

    public static boolean isDirectWaterBucket(ItemStack stack) {
        return stack.is(Items.WATER_BUCKET) || stack.is(PlantClassifier.C_WATER_BUCKETS);
    }

    public static boolean hasFluidWater(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        try {
            var fluidHandler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
            if (fluidHandler != null) {
                for (int i = 0; i < fluidHandler.getTanks(); i++) {
                    var fluidStack = fluidHandler.getFluidInTank(i);
                    if (!fluidStack.isEmpty() && fluidStack.getFluid().isSame(Fluids.WATER) && fluidStack.getAmount() >= 1000) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static int getWaterBuckets(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        if (isDirectWaterBucket(stack)) {
            return stack.getCount();
        }
        int count = 0;
        try {
            var fluidHandler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
            if (fluidHandler != null) {
                for (int t = 0; t < fluidHandler.getTanks(); t++) {
                    var fluid = fluidHandler.getFluidInTank(t);
                    if (!fluid.isEmpty() && fluid.getFluid().isSame(Fluids.WATER)) {
                        count += fluid.getAmount() / 1000;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return count;
    }

    public static boolean drainOneFluidWaterOffhand(Player player, ItemStack stack) {
        if (isDirectWaterBucket(stack)) return false;
        try {
            var fluidHandler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
            if (fluidHandler != null) {
                var drained = fluidHandler.drain(1000, IFluidHandler.FluidAction.EXECUTE);
                if (!drained.isEmpty() && drained.getAmount() >= 1000) {
                    player.setItemInHand(InteractionHand.OFF_HAND, fluidHandler.getContainer());
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static boolean drainOneFluidWaterSlot(Player player, int slotIndex, ItemStack stack) {
        if (isDirectWaterBucket(stack)) return false;
        try {
            var fluidHandler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
            if (fluidHandler != null) {
                var drained = fluidHandler.drain(1000, IFluidHandler.FluidAction.EXECUTE);
                if (!drained.isEmpty() && drained.getAmount() >= 1000) {
                    player.getInventory().setItem(slotIndex, fluidHandler.getContainer());
                    return true;
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
        return BoneMealItem.applyBonemeal(stack, level, pos, player);
    }

    public static SoundType getSoundType(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level != null && pos != null) {
            return state.getSoundType(level, pos, entity);
        }
        return state.getSoundType();
    }
}
