package com.velorise.veinfarming;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class PlatformBridge {
    private PlatformBridge() {}

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static boolean isDirectWaterBucket(ItemStack stack) {
        return stack.is(Items.WATER_BUCKET) || stack.is(PlantClassifier.C_WATER_BUCKETS);
    }

    public static boolean hasFluidWater(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        try {
            ResourceHandler<FluidResource> fluidHandler = ItemAccess.forStack(stack).getCapability(Capabilities.Fluid.ITEM);
            if (fluidHandler != null) {
                FluidResource water = FluidResource.of(Fluids.WATER);
                for (int i = 0; i < fluidHandler.size(); i++) {
                    if (water.equals(fluidHandler.getResource(i)) && fluidHandler.getAmountAsLong(i) >= FluidType.BUCKET_VOLUME) {
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
            ResourceHandler<FluidResource> fluidHandler = ItemAccess.forStack(stack).getCapability(Capabilities.Fluid.ITEM);
            if (fluidHandler != null) {
                FluidResource water = FluidResource.of(Fluids.WATER);
                for (int i = 0; i < fluidHandler.size(); i++) {
                    if (water.equals(fluidHandler.getResource(i))) {
                        count += (int) (fluidHandler.getAmountAsLong(i) / FluidType.BUCKET_VOLUME);
                    }
                }
            }
        } catch (Throwable ignored) {}
        return count;
    }

    public static boolean drainOneFluidWaterOffhand(Player player, ItemStack stack) {
        if (isDirectWaterBucket(stack)) return false;
        try {
            ItemAccess access = ItemAccess.forPlayerInteraction(player, InteractionHand.OFF_HAND).oneByOne();
            ResourceHandler<FluidResource> fluidHandler = access.getCapability(Capabilities.Fluid.ITEM);
            if (fluidHandler != null) {
                try (Transaction tx = Transaction.openRoot()) {
                    int extracted = fluidHandler.extract(FluidResource.of(Fluids.WATER), FluidType.BUCKET_VOLUME, tx);
                    if (extracted >= FluidType.BUCKET_VOLUME) {
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
            ItemAccess access = ItemAccess.forHandlerIndex(PlayerInventoryWrapper.of(player), slotIndex).oneByOne();
            ResourceHandler<FluidResource> fluidHandler = access.getCapability(Capabilities.Fluid.ITEM);
            if (fluidHandler != null) {
                try (Transaction tx = Transaction.openRoot()) {
                    int extracted = fluidHandler.extract(FluidResource.of(Fluids.WATER), FluidType.BUCKET_VOLUME, tx);
                    if (extracted >= FluidType.BUCKET_VOLUME) {
                        tx.commit();
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static boolean isHoe(ItemStack stack) {
        return stack.canPerformAction(ItemAbilities.HOE_TILL) || stack.getItem() instanceof HoeItem;
    }

    public static BlockState getTilledState(Level level, Player player, InteractionHand hand, BlockPos pos, BlockState state) {
        UseOnContext context = new UseOnContext(player, hand, new BlockHitResult(
                Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false
        ));
        return state.getToolModifiedState(context, ItemAbilities.HOE_TILL, false);
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
