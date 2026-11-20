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
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.ModList;

public final class PlatformBridge {
    private PlatformBridge() {}

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static boolean isDirectWaterBucket(ItemStack stack) {
        return stack.is(Items.WATER_BUCKET)
                || stack.is(PlantClassifier.C_WATER_BUCKETS)
                || stack.is(PlantClassifier.FORGE_WATER_BUCKETS);
    }

    public static boolean hasFluidWater(ItemStack stack) {
        if (stack.is(PlantClassifier.FORGE_WATER_BUCKETS)) return true;
        var fluidHandler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        if (fluidHandler != null) {
            for (int i = 0; i < fluidHandler.getTanks(); i++) {
                var fluidStack = fluidHandler.getFluidInTank(i);
                if (!fluidStack.isEmpty() && fluidStack.getFluid().isSame(Fluids.WATER) && fluidStack.getAmount() >= 1000) {
                    return true;
                }
            }
        }
        return false;
    }

    public static int getWaterBuckets(ItemStack stack) {
        if (isDirectWaterBucket(stack)) {
            return stack.getCount();
        }
        var fluidHandler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        int count = 0;
        if (fluidHandler != null) {
            for (int t = 0; t < fluidHandler.getTanks(); t++) {
                var fluid = fluidHandler.getFluidInTank(t);
                if (!fluid.isEmpty() && fluid.getFluid().isSame(Fluids.WATER)) {
                    count += fluid.getAmount() / 1000;
                }
            }
        }
        return count;
    }

    public static boolean drainOneFluidWaterOffhand(Player player, ItemStack stack) {
        if (isDirectWaterBucket(stack)) return false;
        var fluidHandler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        if (fluidHandler != null) {
            var drained = fluidHandler.drain(1000, IFluidHandler.FluidAction.EXECUTE);
            if (!drained.isEmpty() && drained.getAmount() >= 1000) {
                player.setItemInHand(InteractionHand.OFF_HAND, fluidHandler.getContainer());
                return true;
            }
        }
        return false;
    }

    public static boolean drainOneFluidWaterSlot(Player player, int slotIndex, ItemStack stack) {
        if (isDirectWaterBucket(stack)) return false;
        var fluidHandler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        if (fluidHandler != null) {
            var drained = fluidHandler.drain(1000, IFluidHandler.FluidAction.EXECUTE);
            if (!drained.isEmpty() && drained.getAmount() >= 1000) {
                player.getInventory().setItem(slotIndex, fluidHandler.getContainer());
                return true;
            }
        }
        return false;
    }

    public static boolean isHoe(ItemStack stack) {
        return stack.canPerformAction(ToolActions.HOE_TILL) || stack.getItem() instanceof HoeItem;
    }

    public static BlockState getTilledState(Level level, Player player, InteractionHand hand, BlockPos pos, BlockState state) {
        UseOnContext context = new UseOnContext(player, hand, new BlockHitResult(
                Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false
        ));
        return state.getToolModifiedState(context, ToolActions.HOE_TILL, false);
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
