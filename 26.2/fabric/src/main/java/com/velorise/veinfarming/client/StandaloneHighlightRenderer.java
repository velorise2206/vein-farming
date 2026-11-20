package com.velorise.veinfarming.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.velorise.veinfarming.*;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StandaloneHighlightRenderer {

    private record ColoredBox(
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ,
            float r, float g, float b, float a
    ) {
        static ColoredBox from(AABB box, float r, float g, float b, float a) {
            return new ColoredBox(
                    (float) box.minX, (float) box.minY, (float) box.minZ,
                    (float) box.maxX, (float) box.maxY, (float) box.maxZ,
                    r, g, b, a
            );
        }
    }

    private record GhostBlock(
            BlockState state,
            BlockPos lightPos,
            double x, double y, double z,
            float scaleXZ,
            float alpha
    ) {
    }

    private BlockPos lastClickedPos = null;
    private BlockState lastClickedState = null;
    private ItemStack lastHeldItem = ItemStack.EMPTY;
    private ItemStack lastOffItem = ItemStack.EMPTY;
    private long lastCacheTime = 0L;
    private Collection<BlockPos> cachedPreviewBlocks = Collections.emptyList();
    private float cachedR = 1.0f, cachedG = 0.82f, cachedB = 0.2f, cachedA = 0.8f;
    private boolean cachedIsPlanting = false;
    private boolean cachedIsHoe = false;
    private BlockState cachedGhostPlantState = null;
    private BlockState cachedGhostOffState = null;
    private List<BlockPos> cachedPlannedWaterHoles = Collections.emptyList();
    private boolean lastSmartPlantEnabled = true;

    private boolean lastWasAttacking = false;

    private void clearCache() {
        lastClickedPos = null;
        lastClickedState = null;
        lastHeldItem = ItemStack.EMPTY;
        lastOffItem = ItemStack.EMPTY;
        lastWasAttacking = false;
        lastCacheTime = 0L;
        cachedPreviewBlocks = Collections.emptyList();
        cachedIsHoe = false;
        cachedGhostPlantState = null;
        cachedGhostOffState = null;
        cachedPlannedWaterHoles = Collections.emptyList();
    }

    public static final StandaloneHighlightRenderer INSTANCE = new StandaloneHighlightRenderer();

    public static boolean onBlockOutline(LevelRenderContext context, BlockOutlineRenderState outlineState) {
        return INSTANCE.handleBlockOutline(context, outlineState);
    }

    public static void onRenderWorld(LevelRenderContext context) {
        INSTANCE.handleRenderWorld(context);
    }

    public boolean handleBlockOutline(LevelRenderContext context, BlockOutlineRenderState outlineState) {
        if (!FarmingConfig.STANDALONE_PREVIEW.get()) {
            return true;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;
        if (player == null || level == null) {
            return true;
        }
        if (!isClientVeinActive(player)) {
            return true;
        }
        BlockPos clickedPos = outlineState.pos();
        BlockState clickedState = level.getBlockState(clickedPos);
        ItemStack heldItem = player.getMainHandItem();
        ItemStack offItem = player.getOffhandItem();
        if (lastClickedPos != null && lastClickedPos.equals(clickedPos)
                && (cachedPreviewBlocks == null || cachedPreviewBlocks.isEmpty())) {
            return true;
        }
        if (isFarmingAction(level, player, clickedPos, clickedState, heldItem, offItem)) {
            return false;
        }
        return true;
    }

    public static boolean isClientVeinActive(Player player) {
        if (player == null) return false;
        boolean liteLoaded = LiteMinerCompat.isLiteMinerLoaded();
        boolean ftbLoaded = FTBUltimineCompat.isFTBUltimineLoaded();
        if (liteLoaded && LiteMinerCompat.isLiteMinerClientActive()) {
            return true;
        }
        if (ftbLoaded && FTBUltimineCompat.isUltimineClientActive()) {
            return true;
        }
        if (!liteLoaded && !ftbLoaded) {
            return ModKeyMappings.isKeyActive();
        }
        return false;
    }

    public static boolean isFarmingAction(Level level, Player player, BlockPos clickedPos, BlockState clickedState, ItemStack heldItem, ItemStack offItem) {
        if (level == null || player == null || clickedPos == null) {
            return false;
        }

        ItemStack activeSeed = (PlantClassifier.isPlantableSeed(heldItem) || PlantClassifier.isCocoaBean(heldItem))
                ? heldItem
                : ((PlantClassifier.isPlantableSeed(offItem) || PlantClassifier.isCocoaBean(offItem)) ? offItem : ItemStack.EMPTY);

        if (!activeSeed.isEmpty()) {
            if (PlantClassifier.isCocoaBean(activeSeed)) {
                if (PlantClassifier.isJungleLog(clickedState) || clickedState.getBlock() instanceof CocoaBlock) {
                    return true;
                }
            } else if ((FarmingManager.isValidSoilForSeed(activeSeed, clickedState, level, clickedPos)
                            && (level.getBlockState(clickedPos.above()).isAir() || level.getBlockState(clickedPos.above()).canBeReplaced()))
                    || (FarmingManager.isValidSoilForSeed(activeSeed, level.getBlockState(clickedPos.below()), level, clickedPos.below())
                            && (clickedState.isAir() || clickedState.canBeReplaced()))) {
                return true;
            }
        }

        BlockPos targetHoePos = clickedPos;
        if (PlantClassifier.isFoliageOrPlant(clickedState) && FarmingManager.isTillable(level, player, InteractionHand.MAIN_HAND, targetHoePos.below())) {
            targetHoePos = targetHoePos.below();
        }
        if (FarmingEventHandler.isHoe(heldItem) && FarmingManager.isTillable(level, player, InteractionHand.MAIN_HAND, targetHoePos)) {
            return true;
        }

        if (heldItem.is(Items.BONE_MEAL)) {
            if (FarmingManager.isCrop(clickedState) || FarmingManager.isFarmland(clickedState)
                    || FarmingManager.isBonemealCrop(clickedState)
                    || FarmingManager.isBonemealCrop(level.getBlockState(clickedPos.above()))
                    || FarmingManager.isSmallFlower(clickedState)) {
                return true;
            }
        }

        if (clickedState.is(Blocks.COMPOSTER) && FarmingManager.isCompostable(heldItem)) {
            return true;
        }

        BlockPos targetCrop = clickedPos;
        if (FarmingManager.isRiceCrop(clickedState)) {
            if (FarmingManager.isRiceCrop(level.getBlockState(clickedPos.above()))) {
                targetCrop = clickedPos.above();
            }
        } else if (!FarmingManager.isCrop(clickedState) && !FarmingManager.isColumnCrop(clickedState) && !FarmingManager.isFruitCrop(clickedState) && !FarmingManager.isHarvestablePlant(clickedState)) {
            targetCrop = clickedPos.above();
        }
        BlockState targetCropState = level.getBlockState(targetCrop);

        return FarmingManager.isHarvestablePlant(targetCropState)
                || FarmingManager.isHarvestablePlant(clickedState)
                || FarmingManager.isRiceCrop(targetCropState)
                || FarmingManager.isRiceCrop(clickedState);
    }

    public void handleRenderWorld(LevelRenderContext context) {
        if (!FarmingConfig.STANDALONE_PREVIEW.get()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !(mc.level instanceof ClientLevel level)) {
            clearCache();
            return;
        }

        if (!isClientVeinActive(player)) {
            clearCache();
            return;
        }

        boolean currentSmartPlant = ModKeyMappings.isSmartPlantEnabled();
        if (currentSmartPlant != lastSmartPlantEnabled) {
            clearCache();
            lastSmartPlantEnabled = currentSmartPlant;
        }

        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            clearCache();
            return;
        }

        BlockPos clickedPos = blockHit.getBlockPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        ItemStack heldItem = player.getMainHandItem();
        ItemStack offItem = player.getOffhandItem();
        InteractionHand hand = InteractionHand.MAIN_HAND;
        ItemStack activeSeed = (PlantClassifier.isPlantableSeed(heldItem) || PlantClassifier.isCocoaBean(heldItem))
                ? heldItem
                : ((PlantClassifier.isPlantableSeed(offItem) || PlantClassifier.isCocoaBean(offItem)) ? offItem : ItemStack.EMPTY);
        BlockPos initialHoePos = clickedPos;
        if (PlantClassifier.isFoliageOrPlant(clickedState) && FarmingManager.isTillable(level, player, hand, initialHoePos.below())) {
            initialHoePos = initialHoePos.below();
        }
        final BlockPos targetHoePos = initialHoePos;

        boolean isCompanionLoaded = LiteMinerCompat.isLiteMinerLoaded() || FTBUltimineCompat.isFTBUltimineLoaded();
        boolean farmingAction = isFarmingAction(level, player, clickedPos, clickedState, heldItem, offItem);

        if (isCompanionLoaded && !farmingAction) {
            clearCache();
            return;
        }

        long now = System.currentTimeMillis();
        boolean isAttacking = mc.options.keyAttack.isDown();
        boolean cacheValid = lastClickedPos != null
                && lastClickedPos.equals(clickedPos)
                && clickedState.equals(lastClickedState)
                && ItemStack.matches(heldItem, lastHeldItem)
                && ItemStack.matches(offItem, lastOffItem)
                && (isAttacking == lastWasAttacking)
                && (now - lastCacheTime < 250);

        Collection<BlockPos> previewBlocks;
        List<BlockPos> plannedWaterHoles;
        float r, g, b, a;
        boolean isPlanting;
        boolean isHoe;
        BlockState ghostPlantState;
        BlockState ghostOffState;

        if (cacheValid) {
            previewBlocks = cachedPreviewBlocks;
            plannedWaterHoles = cachedPlannedWaterHoles;
            r = cachedR; g = cachedG; b = cachedB; a = cachedA;
            isPlanting = cachedIsPlanting;
            isHoe = cachedIsHoe;
            ghostPlantState = cachedGhostPlantState;
            ghostOffState = cachedGhostOffState;
        } else {
            previewBlocks = Collections.emptyList();
            plannedWaterHoles = Collections.emptyList();
            r = 1.0f; g = 0.82f; b = 0.2f; a = 0.8f;
            isPlanting = false;
            isHoe = false;
            ghostPlantState = null;
            ghostOffState = null;

            BlockPos targetCrop = clickedPos;
            if (FarmingManager.isRiceCrop(clickedState)) {
                if (FarmingManager.isRiceCrop(level.getBlockState(clickedPos.above()))) {
                    targetCrop = clickedPos.above();
                }
            } else if (!FarmingManager.isCrop(clickedState) && !FarmingManager.isColumnCrop(clickedState) && !FarmingManager.isFruitCrop(clickedState) && !FarmingManager.isHarvestablePlant(clickedState)) {
                targetCrop = clickedPos.above();
            }
            BlockState targetCropState = level.getBlockState(targetCrop);

            boolean isHarvestable = FarmingManager.isMatureCrop(targetCropState)
                    || FarmingManager.isFruitCrop(targetCropState)
                    || FarmingManager.isFruitCrop(clickedState)
                    || FarmingManager.isColumnCrop(targetCropState)
                    || FarmingManager.isChorus(targetCropState)
                    || FarmingManager.isChorus(clickedState)
                    || (FarmingManager.isRiceCrop(targetCropState) && FarmingManager.isMatureCrop(targetCropState));

            boolean isPlant = FarmingManager.isHarvestablePlant(targetCropState)
                    || FarmingManager.isHarvestablePlant(clickedState)
                    || FarmingManager.isRiceCrop(targetCropState)
                    || FarmingManager.isRiceCrop(clickedState);

            boolean isTool = FarmingManager.isDestructionTool(heldItem);

            if (isPlant && (isAttacking || isTool || !isHarvestable)) {
                Collection<BlockPos> companionBlocks = Collections.emptyList();
                if (isCompanionLoaded) {
                    companionBlocks = FarmingManager.getClientSelectedPositions(level, player, targetCrop);
                }
                if (companionBlocks != null && !companionBlocks.isEmpty()) {
                    List<BlockPos> targets = new ArrayList<>();
                    for (BlockPos p : companionBlocks) {
                        if (FarmingManager.isHarvestablePlant(level.getBlockState(p))) {
                            targets.add(p);
                        }
                    }
                    previewBlocks = targets;
                } else {
                    previewBlocks = FarmingManager.findDestroyTargets(level, targetCrop, targetCropState);
                }
                r = 1.0f; g = 0.22f; b = 0.22f; a = 0.85f;
            } else if (isHarvestable) {
                Collection<BlockPos> companionBlocks = Collections.emptyList();
                if (isCompanionLoaded) {
                    companionBlocks = FarmingManager.getClientSelectedPositions(level, player, targetCrop);
                }
                if (companionBlocks != null && !companionBlocks.isEmpty()) {
                    List<BlockPos> targets = new ArrayList<>();
                    for (BlockPos p : companionBlocks) {
                        BlockState s = level.getBlockState(p);
                        if (FarmingManager.isMatureCrop(s) || FarmingManager.isFruitCrop(s) || FarmingManager.isColumnCrop(s) || FarmingManager.isChorus(s)) {
                            targets.add(p);
                        }
                    }
                    previewBlocks = targets;
                } else {
                    previewBlocks = FarmingManager.fallbackHarvestSearch(level, targetCrop);
                }
                if ((previewBlocks == null || previewBlocks.isEmpty()) && isPlant) {
                    previewBlocks = FarmingManager.findDestroyTargets(level, targetCrop, targetCropState);
                    r = 1.0f; g = 0.22f; b = 0.22f; a = 0.85f;
                } else {
                    r = 1.0f; g = 0.82f; b = 0.2f; a = 0.8f;
                }
            } else if (clickedState.is(Blocks.COMPOSTER) && FarmingConfig.BATCH_COMPOSTER.get() && FarmingManager.isCompostable(heldItem)) {
                previewBlocks = Collections.singletonList(clickedPos);
                r = 0.45f; g = 0.75f; b = 0.25f;
            } else if (FarmingEventHandler.isHoe(heldItem) && FarmingManager.isTillable(level, player, hand, targetHoePos)) {
                Collection<BlockPos> companionBlocks = Collections.emptyList();
                if (isCompanionLoaded) {
                    companionBlocks = FarmingManager.getClientSelectedPositions(level, player, targetHoePos);
                }
                if (companionBlocks != null && !companionBlocks.isEmpty()) {
                    List<BlockPos> tillable = new ArrayList<>();
                    for (BlockPos p : companionBlocks) {
                        if (FarmingManager.isTillable(level, player, hand, p)) {
                            tillable.add(p);
                        }
                    }
                    tillable.sort(Comparator.comparingInt(p -> p.distManhattan(targetHoePos)));
                    previewBlocks = tillable;
                } else {
                    previewBlocks = FarmingManager.fallbackHoeSearch(player, hand, targetHoePos);
                }
                r = 0.65f; g = 0.45f; b = 0.25f;
                isHoe = true;
                if (PlantClassifier.isWaterContainer(offItem) && FarmingConfig.SMART_WATER_BUCKET_IRRIGATION.get() && previewBlocks.size() >= 2) {
                    int availableWater = FarmingManager.getAvailableWaterCount(player);
                    plannedWaterHoles = FarmingManager.calculateOptimalWaterHoles(level, previewBlocks, targetHoePos, availableWater);
                }
            } else if (!activeSeed.isEmpty() && (
                    FarmingManager.isCocoaBean(activeSeed)
                        ? (FarmingManager.isJungleLog(clickedState) || clickedState.getBlock() instanceof CocoaBlock)
                        : ((FarmingManager.isValidSoilForSeed(activeSeed, clickedState, level, clickedPos) && (level.getBlockState(clickedPos.above()).isAir() || level.getBlockState(clickedPos.above()).canBeReplaced()))
                           || (FarmingManager.isValidSoilForSeed(activeSeed, level.getBlockState(clickedPos.below()), level, clickedPos.below()) && (clickedState.isAir() || clickedState.canBeReplaced())))
            )) {
                BlockPos startSoilPos = FarmingManager.isValidSoilForSeed(activeSeed, clickedState, level, clickedPos) ? clickedPos : clickedPos.below();
                Collection<BlockPos> companionBlocks = Collections.emptyList();
                if (isCompanionLoaded) {
                    companionBlocks = FarmingManager.getClientSelectedPositions(level, player, startSoilPos);
                    if (companionBlocks == null || companionBlocks.isEmpty()) {
                        companionBlocks = FarmingManager.getClientSelectedPositions(level, player, clickedPos);
                    }
                }
                if (companionBlocks != null && !companionBlocks.isEmpty()) {
                    previewBlocks = PlantingManager.filterSelectedPlantingPositions(level, companionBlocks, startSoilPos, activeSeed, ModKeyMappings.isSmartPlantEnabled());
                } else {
                    previewBlocks = PlantingManager.fallbackPlantingSearch(level, clickedPos, activeSeed, ModKeyMappings.isSmartPlantEnabled());
                }
                r = 0.4f; g = 0.85f; b = 0.3f;
                isPlanting = true;
                if (FarmingConfig.GHOST_PLANT_PREVIEW.get()) {
                    ghostPlantState = getPlantedBlockState(activeSeed);
                    if (activeSeed == heldItem
                            && ModKeyMappings.isSmartPlantEnabled()
                            && FarmingConfig.SMART_INTERCROPPING.get()
                            && FarmingManager.isIntercroppableCrop(heldItem)
                            && FarmingManager.isIntercroppableCrop(offItem)
                            && !heldItem.is(offItem.getItem())) {
                        ghostOffState = getPlantedBlockState(offItem);
                    }
                }
            } else if (heldItem.is(Items.BONE_MEAL)) {
                if (FarmingManager.isCrop(clickedState) || FarmingManager.isFarmland(clickedState)
                        || FarmingManager.isBonemealCrop(clickedState)
                        || FarmingManager.isBonemealCrop(level.getBlockState(clickedPos.above()))
                        || FarmingManager.isSmallFlower(clickedState)) {
                    BlockPos targetPos = FarmingManager.isFarmland(clickedState) ? clickedPos.above() : clickedPos;
                    Collection<BlockPos> companionBlocks = Collections.emptyList();
                    if (isCompanionLoaded) {
                        companionBlocks = FarmingManager.getClientSelectedPositions(level, player, targetPos);
                        if (companionBlocks == null || companionBlocks.isEmpty()) {
                            companionBlocks = FarmingManager.getClientSelectedPositions(level, player, targetPos.below());
                        }
                    }
                    if (companionBlocks != null && !companionBlocks.isEmpty()) {
                        List<BlockPos> bonemealable = new ArrayList<>();
                        for (BlockPos p : companionBlocks) {
                            BlockState s = level.getBlockState(p);
                            if (FarmingManager.isCrop(s) || FarmingManager.isBonemealCrop(s) || FarmingManager.isSmallFlower(s)) {
                                bonemealable.add(p);
                            }
                        }
                        previewBlocks = bonemealable;
                    } else {
                        previewBlocks = FarmingManager.fallbackCropSearch(level, clickedPos);
                    }
                    if (FarmingManager.isSmallFlower(clickedState)) {
                        r = 0.95f; g = 0.55f; b = 0.85f;
                    } else {
                        r = 0.3f; g = 0.9f; b = 0.4f;
                    }
                }
            }

            lastClickedPos = clickedPos;
            lastClickedState = clickedState;
            lastHeldItem = heldItem.copy();
            lastOffItem = offItem.copy();
            lastWasAttacking = isAttacking;
            lastCacheTime = now;
            cachedPreviewBlocks = previewBlocks;
            cachedPlannedWaterHoles = plannedWaterHoles;
            cachedR = r; cachedG = g; cachedB = b; cachedA = a;
            cachedIsPlanting = isPlanting;
            cachedIsHoe = isHoe;
            cachedGhostPlantState = ghostPlantState;
            cachedGhostOffState = ghostOffState;
        }

        if (previewBlocks == null || previewBlocks.isEmpty()) {
            return;
        }

        SubmitNodeCollector submitNodeCollector = context.submitNodeCollector();
        if (submitNodeCollector == null) {
            return;
        }

        PoseStack poseStack = context.poseStack() != null ? context.poseStack() : new PoseStack();
        Vec3 camPos = (context.levelState() != null && context.levelState().cameraRenderState != null)
                ? context.levelState().cameraRenderState.pos
                : mc.gameRenderer.mainCamera().position();

        Map<BlockPos, Block> plannedIntercrop = null;
        if (isPlanting && ghostOffState != null && ModKeyMappings.isSmartPlantEnabled()) {
            BlockPos originSoilPos = FarmingManager.isValidSoilForSeed(activeSeed, clickedState, level, clickedPos) ? clickedPos : clickedPos.below();
            Block mainCropBlock = FarmingManager.getCropBlock(activeSeed);
            Block offCropBlock = FarmingManager.getCropBlock(offItem);
            boolean evenIsMain = FarmingManager.determineIntercropPhase(level, originSoilPos, player.getDirection(), mainCropBlock, offCropBlock);
            boolean alternateOnX = (player.getDirection().getAxis() == Direction.Axis.Z);

            plannedIntercrop = new HashMap<>();
            for (BlockPos p : previewBlocks) {
                int rowCoord = alternateOnX ? (p.getX() - originSoilPos.getX()) : (p.getZ() - originSoilPos.getZ());
                boolean isMainRow = (Math.floorMod(rowCoord, 2) == 0) ? evenIsMain : !evenIsMain;
                plannedIntercrop.put(p.above(), isMainRow ? mainCropBlock : offCropBlock);
            }
        }

        List<ColoredBox> lineBoxes = new ArrayList<>(previewBlocks.size());
        List<GhostBlock> ghostBlocks = new ArrayList<>();

        float lineAlpha = ((isPlanting && ghostPlantState != null) || (isHoe && FarmingConfig.GHOST_FARMLAND_PREVIEW.get())) ? 0.35f : a;
        boolean isCocoaPlanting = isPlanting && FarmingManager.isCocoaBean(activeSeed);
        Set<BlockPos> waterHoleSet = plannedWaterHoles.isEmpty() ? Collections.emptySet() : new HashSet<>(plannedWaterHoles);

        for (BlockPos pos : previewBlocks) {
            BlockPos renderPos = (isPlanting && !isCocoaPlanting) ? pos.above() : pos;
            AABB aabb;
            if (isPlanting) {
                aabb = new AABB(
                        renderPos.getX() + 0.002, renderPos.getY() + 0.003, renderPos.getZ() + 0.002,
                        renderPos.getX() + 0.998, renderPos.getY() + 0.998, renderPos.getZ() + 0.998
                );
            } else {
                BlockState state = level.getBlockState(renderPos);
                VoxelShape shape = state.getShape(level, renderPos);
                aabb = shape.isEmpty()
                        ? new AABB(renderPos)
                        : shape.bounds().move(renderPos);
                if (isHoe) {
                    double coverTop = renderPos.getY() + getHoePreviewTopY(level, renderPos);
                    if (coverTop > aabb.maxY) {
                        aabb = new AABB(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, coverTop, aabb.maxZ);
                    }
                    aabb = new AABB(
                            aabb.minX - 0.002, aabb.minY - 0.002, aabb.minZ - 0.002,
                            aabb.maxX + 0.002, aabb.maxY + 0.007, aabb.maxZ + 0.002
                    );
                } else {
                    aabb = aabb.inflate(0.002);
                }
            }
            AABB camRelative = aabb.move(-camPos.x, -camPos.y, -camPos.z);

            float boxR = r, boxG = g, boxB = b;
            float currentAlpha = lineAlpha;
            if (isHoe && FarmingConfig.SMART_IRRIGATION_PREVIEW.get()) {
                if (waterHoleSet.contains(pos)) {
                    boxR = 0.15f; boxG = 0.65f; boxB = 1.0f;
                    currentAlpha = 0.85f;
                } else {
                    boolean hasWater = FarmingManager.isNearWater(level, pos) || FarmingManager.isHydratedByHoles(pos, plannedWaterHoles);
                    if (hasWater) {
                        boxR = 0.45f; boxG = 0.65f; boxB = 0.35f;
                        currentAlpha = 0.40f;
                    } else {
                        boxR = 0.95f; boxG = 0.45f; boxB = 0.15f;
                        currentAlpha = 0.65f;
                    }
                }
            } else if (isPlanting && plannedIntercrop != null && FarmingConfig.GROWTH_PENALTY_WARNING.get()) {
                Block plannedCrop = plannedIntercrop.get(renderPos);
                boolean hasPenalty = FarmingManager.hasGrowthPenalty(level, renderPos, plannedCrop, plannedIntercrop);
                if (hasPenalty) {
                    boxR = 1.0f; boxG = 0.28f; boxB = 0.2f;
                    currentAlpha = 0.85f;
                } else {
                    boxR = 0.35f; boxG = 0.85f; boxB = 0.3f;
                    currentAlpha = 0.35f;
                }
            }

            lineBoxes.add(ColoredBox.from(camRelative, boxR, boxG, boxB, currentAlpha));
        }

        // Real 3D ghost block models for Farmland (wet/dry) and planned Water Holes when holding a hoe
        if (isHoe && FarmingConfig.GHOST_FARMLAND_PREVIEW.get()) {
            BlockState waterGhost = Blocks.ICE.defaultBlockState();
            BlockState wetFarmland = Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7);
            BlockState dryFarmland = Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 0);

            for (BlockPos pos : previewBlocks) {
                double blockTop = getHoePreviewTopY(level, pos);

                if (waterHoleSet.contains(pos)) {
                    double yOffset = (blockTop - 1.0) + 0.006;
                    ghostBlocks.add(new GhostBlock(
                            waterGhost, pos.above(),
                            pos.getX() - camPos.x, pos.getY() - camPos.y + yOffset, pos.getZ() - camPos.z,
                            0.998f, 0.65f
                    ));
                } else {
                    double yOffset = (blockTop - 0.9375) + 0.006;
                    boolean hasWater = FarmingManager.isNearWater(level, pos) || FarmingManager.isHydratedByHoles(pos, plannedWaterHoles);
                    ghostBlocks.add(new GhostBlock(
                            hasWater ? wetFarmland : dryFarmland, pos.above(),
                            pos.getX() - camPos.x, pos.getY() - camPos.y + yOffset, pos.getZ() - camPos.z,
                            0.998f, hasWater ? 0.70f : 0.50f
                    ));
                }
            }
        }

        // Real 3D ghost block models for planted crops, saplings, cocoa, flowers, etc.
        if (ghostPlantState != null) {
            BlockPos originSoilPos = FarmingManager.isValidSoilForSeed(activeSeed, clickedState, level, clickedPos) ? clickedPos : clickedPos.below();
            Block mainCropBlock = FarmingManager.getCropBlock(activeSeed);
            Block offCropBlock = FarmingManager.getCropBlock(offItem);

            boolean evenIsMain = true;
            if (ghostOffState != null) {
                evenIsMain = FarmingManager.determineIntercropPhase(level, originSoilPos, player.getDirection(), mainCropBlock, offCropBlock);
            }
            boolean alternateOnX = (player.getDirection().getAxis() == Direction.Axis.Z);
            for (BlockPos pos : previewBlocks) {
                BlockState stateToRender = ghostPlantState;
                if (isCocoaPlanting) {
                    Direction facing = Direction.NORTH;
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        if (FarmingManager.isJungleLog(level.getBlockState(pos.relative(d)))) {
                            facing = d;
                            break;
                        }
                    }
                    stateToRender = Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.FACING, facing).setValue(CocoaBlock.AGE, 0);
                } else if (ghostOffState != null) {
                    int rowCoord = alternateOnX ? (pos.getX() - originSoilPos.getX()) : (pos.getZ() - originSoilPos.getZ());
                    boolean isMainRow = (Math.floorMod(rowCoord, 2) == 0) ? evenIsMain : !evenIsMain;
                    stateToRender = isMainRow ? ghostPlantState : ghostOffState;
                }
                if (stateToRender == null) continue;

                BlockPos plantPos = isCocoaPlanting ? pos : pos.above();
                if (isCameraInsideBlock(camPos, plantPos)) continue;
                double plantYOffset = isCocoaPlanting ? 0.0 : 0.004;
                float plantScaleXZ = isCocoaPlanting ? 1.0f : 0.996f;
                ghostBlocks.add(new GhostBlock(
                        stateToRender, plantPos,
                        plantPos.getX() - camPos.x, plantPos.getY() - camPos.y + plantYOffset, plantPos.getZ() - camPos.z,
                        plantScaleXZ, 0.45f
                ));
            }
        }

        // Real 3D ghost block models for reserved Melon / Pumpkin fruit spots
        if (isPlanting && FarmingManager.isFruitSeed(activeSeed)) {
            Block fruitBlock = FarmingManager.getFruitForSeed(activeSeed);
            if (fruitBlock != null) {
                BlockState fruitState = fruitBlock.defaultBlockState();
                Set<BlockPos> fruitSpots = new HashSet<>();
                Set<BlockPos> stemSoilSet = new HashSet<>(previewBlocks);

                for (BlockPos soil : previewBlocks) {
                    for (Direction dir : Direction.Plane.HORIZONTAL) {
                        BlockPos neighborSoil = soil.relative(dir);
                        if (!stemSoilSet.contains(neighborSoil)) {
                            BlockState neighborSoilState = level.getBlockState(neighborSoil);
                            if (FarmingManager.isFarmland(neighborSoilState) || neighborSoilState.is(net.minecraft.tags.BlockTags.DIRT)) {
                                BlockPos fruitPos = neighborSoil.above();
                                BlockState fruitPosState = level.getBlockState(fruitPos);
                                if (fruitPosState.isAir() || fruitPosState.canBeReplaced()) {
                                    fruitSpots.add(fruitPos);
                                }
                            }
                        }
                    }
                }

                for (BlockPos fruitPos : fruitSpots) {
                    if (isCameraInsideBlock(camPos, fruitPos)) continue;
                    ghostBlocks.add(new GhostBlock(
                            fruitState, fruitPos,
                            fruitPos.getX() - camPos.x, fruitPos.getY() - camPos.y + 0.004, fruitPos.getZ() - camPos.z,
                            0.994f, 0.35f
                    ));
                }
            }
        }

        // Submit wireframes through SubmitNodeCollector
        if (!lineBoxes.isEmpty()) {
            submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, consumer) -> {
                for (ColoredBox box : lineBoxes) {
                    renderLineBox(pose, consumer, box);
                }
            });
        }

        // Submit real textured, tinted, translucent 3D block models through SubmitNodeCollector
        if (!ghostBlocks.isEmpty()) {
            submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentCullItemTarget(TextureAtlas.LOCATION_BLOCKS), (pose, buffer) -> {
                ModelBlockRenderer blockRenderer = new ModelBlockRenderer(false, false, mc.getBlockColors());
                PoseStack localStack = new PoseStack();
                localStack.last().pose().set(pose.pose());
                localStack.last().normal().set(pose.normal());

                for (GhostBlock ghost : ghostBlocks) {
                    BlockStateModel model = mc.getModelManager().getBlockStateModelSet().get(ghost.state());
                    int alphaInt = Math.round(ghost.alpha() * 255.0F);

                    localStack.pushPose();
                    if (ghost.scaleXZ() != 1.0f) {
                        localStack.translate(ghost.x() + 0.5, ghost.y(), ghost.z() + 0.5);
                        localStack.scale(ghost.scaleXZ(), 1.0f, ghost.scaleXZ());
                        localStack.translate(-0.5, 0.0, -0.5);
                    } else {
                        localStack.translate(ghost.x(), ghost.y(), ghost.z());
                    }

                    PoseStack.Pose blockPose = localStack.last();
                    blockRenderer.tesselateBlock(
                            (x, y, z, quad, instance) -> {
                                for (int v = 0; v < 4; v++) {
                                    int color = instance.getColor(v);
                                    instance.setColor(v, ARGB.color(alphaInt, ARGB.red(color), ARGB.green(color), ARGB.blue(color)));
                                }
                                buffer.putBakedQuad(blockPose, quad, instance);
                            },
                            0, 0, 0,
                            level,
                            ghost.lightPos(),
                            ghost.state(),
                            model,
                            ghost.state().getSeed(ghost.lightPos())
                    );
                    localStack.popPose();
                }
            });
        }
    }

    /**
     * Returns the effective top Y (relative to pos.getY()) for hoe wireframe & ghost preview.
     * Lifts the preview just above low flat surface covers (e.g. snow layers, leaf litter, petals)
     * that will be cleared on till so the cover does not occlude the preview.
     */

    private static boolean isCameraInsideBlock(Vec3 camPos, BlockPos pos) {
        return camPos.x >= pos.getX() - 0.15 && camPos.x <= pos.getX() + 1.15
                && camPos.y >= pos.getY() - 0.15 && camPos.y <= pos.getY() + 1.15
                && camPos.z >= pos.getZ() - 0.15 && camPos.z <= pos.getZ() + 1.15;
    }

    private static double getHoePreviewTopY(Level level, BlockPos pos) {
        BlockState targetState = level.getBlockState(pos);
        VoxelShape shape = targetState.getShape(level, pos);
        double blockTop = shape.isEmpty() ? 1.0 : shape.max(Direction.Axis.Y);

        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        if (!aboveState.isAir() && PlantClassifier.isFoliageOrPlant(aboveState)) {
            VoxelShape aboveShape = aboveState.getShape(level, abovePos);
            if (!aboveShape.isEmpty()) {
                double coverHeight = aboveShape.max(Direction.Axis.Y);
                if (coverHeight > 0.0 && coverHeight <= 0.5) {
                    blockTop = Math.max(blockTop, 1.0 + coverHeight);
                }
            }
        }
        return blockTop;
    }

    public static BlockState getPlantedBlockState(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        if (stack.is(Items.BAMBOO)) {
            return Blocks.BAMBOO_SAPLING.defaultBlockState();
        }
        if (stack.is(Items.SWEET_BERRIES)) {
            return Blocks.SWEET_BERRY_BUSH.defaultBlockState();
        }
        if (stack.is(Items.NETHER_WART)) {
            return Blocks.NETHER_WART.defaultBlockState();
        }
        if (stack.is(Items.COCOA_BEANS)) {
            return Blocks.COCOA.defaultBlockState();
        }
        if (stack.is(Items.CHORUS_FLOWER)) {
            return Blocks.CHORUS_FLOWER.defaultBlockState();
        }
        if (stack.is(Items.RED_MUSHROOM)) {
            return Blocks.RED_MUSHROOM.defaultBlockState();
        }
        if (stack.is(Items.BROWN_MUSHROOM)) {
            return Blocks.BROWN_MUSHROOM.defaultBlockState();
        }
        if (stack.is(Items.CRIMSON_FUNGUS)) {
            return Blocks.CRIMSON_FUNGUS.defaultBlockState();
        }
        if (stack.is(Items.WARPED_FUNGUS)) {
            return Blocks.WARPED_FUNGUS.defaultBlockState();
        }
        if (stack.getItem() instanceof BlockItem bi) {
            Block b = bi.getBlock();
            if (b == Blocks.BAMBOO) {
                return Blocks.BAMBOO_SAPLING.defaultBlockState();
            }
            return b.defaultBlockState();
        }
        return null;
    }

    private static void renderLineBox(PoseStack.Pose pose, VertexConsumer consumer, ColoredBox box) {
        float minX = box.minX(), minY = box.minY(), minZ = box.minZ();
        float maxX = box.maxX(), maxY = box.maxY(), maxZ = box.maxZ();
        float r = box.r(), g = box.g(), b = box.b(), a = box.a();

        drawBoxLine(consumer, pose, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        drawBoxLine(consumer, pose, minX, minY, minZ, minX, minY, maxZ, r, g, b, a);
        drawBoxLine(consumer, pose, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        drawBoxLine(consumer, pose, minX, minY, maxZ, maxX, minY, maxZ, r, g, b, a);

        drawBoxLine(consumer, pose, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        drawBoxLine(consumer, pose, minX, maxY, minZ, minX, maxY, maxZ, r, g, b, a);
        drawBoxLine(consumer, pose, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        drawBoxLine(consumer, pose, minX, maxY, maxZ, maxX, maxY, maxZ, r, g, b, a);

        drawBoxLine(consumer, pose, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        drawBoxLine(consumer, pose, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        drawBoxLine(consumer, pose, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
        drawBoxLine(consumer, pose, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
    }

    private static void drawBoxLine(VertexConsumer consumer, PoseStack.Pose pose,
                                    float x1, float y1, float z1, float x2, float y2, float z2,
                                    float r, float g, float b, float a) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-4F) {
            dx = 0; dy = 1; dz = 0;
        } else {
            dx /= len; dy /= len; dz /= len;
        }
        consumer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a).setNormal(pose, dx, dy, dz).setLineWidth(2.0F);
        consumer.addVertex(pose, x2, y2, z2).setColor(r, g, b, a).setNormal(pose, dx, dy, dz).setLineWidth(2.0F);
    }
}
