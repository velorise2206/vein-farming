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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.ItemAbilities;

public final class PlatformBridge {
    private PlatformBridge() {}

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static boolean isDirectWaterBucket(ItemStack stack) {
        return stack.is(Items.WATER_BUCKET) || stack.is(PlantClassifier.C_WATER_BUCKETS);
    }

    public static boolean hasFluidWater(ItemStack stack) {
        return false;
    }

    public static int getWaterBuckets(ItemStack stack) {
        return isDirectWaterBucket(stack) ? stack.getCount() : 0;
    }

    public static boolean drainOneFluidWaterOffhand(Player player, ItemStack stack) {
        return false;
    }

    public static boolean drainOneFluidWaterSlot(Player player, int slotIndex, ItemStack stack) {
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
