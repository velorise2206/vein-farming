package com.velorise.veinfarming.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.velorise.veinfarming.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

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
        if (isFarmingAction(level, player, clickedPos, clickedState, heldItem, offItem)) {
            // Cancel vanilla block highlight AND suppress companion mod basic wireframes
            // so Vein Farming's rich preview and ghost models can render cleanly without visual collision!
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

        // 1. Planting (Seeds, saplings, cocoa, flora, mushrooms, chorus flower) - main hand or offhand
        ItemStack activeSeed = (PlantClassifier.isPlantableSeed(heldItem) || PlantClassifier.isCocoaBean(heldItem))
                ? heldItem
                : ((PlantClassifier.isPlantableSeed(offItem) || PlantClassifier.isCocoaBean(offItem)) ? offItem : ItemStack.EMPTY);

        if (!activeSeed.isEmpty()) {
            if (PlantClassifier.isCocoaBean(activeSeed)) {
                if (PlantClassifier.isJungleLog(clickedState) || clickedState.getBlock() instanceof CocoaBlock) {
                    return true;
                }
            } else if (FarmingManager.isValidSoilForSeed(activeSeed, clickedState, level, clickedPos)
                    || FarmingManager.isValidSoilForSeed(activeSeed, level.getBlockState(clickedPos.below()), level, clickedPos.below())) {
                return true;
            }
        }

        // 2. Hoeing
        BlockPos targetHoePos = clickedPos;
        if (PlantClassifier.isFoliageOrPlant(clickedState) && FarmingManager.isTillable(level, player, InteractionHand.MAIN_HAND, targetHoePos.below())) {
            targetHoePos = targetHoePos.below();
        }
        if (FarmingEventHandler.isHoe(heldItem) && FarmingManager.isTillable(level, player, InteractionHand.MAIN_HAND, targetHoePos)) {
            return true;
        }

        // 3. Bone meal
        if (heldItem.is(Items.BONE_MEAL)) {
            return true;
        }

        // 4. Batch Composter
        if (clickedState.is(Blocks.COMPOSTER) && FarmingManager.isCompostable(heldItem)) {
            return true;
        }

        // 5. Crops & Agricultural Plants
        BlockPos targetCrop = clickedPos;
        if (FarmingManager.isRiceCrop(clickedState)) {
            if (FarmingManager.isRiceCrop(level.getBlockState(clickedPos.above()))) {
                targetCrop = clickedPos.above();
            }
        } else if (!FarmingManager.isCrop(clickedState) && !FarmingManager.isColumnCrop(clickedState) && !FarmingManager.isFruitCrop(clickedState) && !FarmingManager.isHarvestablePlant(clickedState)) {
            targetCrop = clickedPos.above();
        }
        BlockState targetCropState = level.getBlockState(targetCrop);

        if (FarmingManager.isHarvestablePlant(targetCropState)
                || FarmingManager.isHarvestablePlant(clickedState)
                || FarmingManager.isRiceCrop(targetCropState)
                || FarmingManager.isRiceCrop(clickedState)) {
            return true;
        }

        return false;
    }

    public void handleRenderWorld(LevelRenderContext context) {

        if (!FarmingConfig.STANDALONE_PREVIEW.get()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;
        if (player == null || level == null) {
            clearCache();
            return;
        }

        // Fast-path exit when vein key is not held to avoid per-frame block/item checks
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

        // When a companion mod is loaded, we ONLY override agricultural actions (planting, saplings, crops, tilling, harvest).
        // Non-farming actions (mining stone, cutting trees with axe) are left to LiteMiner / FTB Ultimine.
        if (isCompanionLoaded && !farmingAction) {
            clearCache();
            return;
        }

        // Check if cached search result is still valid (throttle search to at most once per 250ms when looking at same block)
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
            r = 1.0f; g = 0.82f; b = 0.2f; a = 0.8f; // Default golden harvest
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
                // DESTROY PREVIEW (Left-click / Breaking Mode) - Vivid Crimson Red
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
                // HARVEST PREVIEW (Right-click Harvest Mode) - Golden Autumn Harvest
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
                r = 1.0f; g = 0.82f; b = 0.2f; a = 0.8f;
            } else if (clickedState.is(Blocks.COMPOSTER) && FarmingConfig.BATCH_COMPOSTER.get() && FarmingManager.isCompostable(heldItem)) {
                previewBlocks = Collections.singletonList(clickedPos);
                r = 0.45f; g = 0.75f; b = 0.25f; // Compost Green
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
                r = 0.65f; g = 0.45f; b = 0.25f; // Earth Farmland Brown
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
                r = 0.4f; g = 0.85f; b = 0.3f; // Sprout Green
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
                        r = 0.95f; g = 0.55f; b = 0.85f; // Flora Blossom Pink
                    } else {
                        r = 0.3f; g = 0.9f; b = 0.4f; // Emerald Jade
                    }
                }
            }

            // Update cache
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

        PoseStack poseStack = context.poseStack();
        if (poseStack == null) {
            return;
        }
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.position();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.lines());

        poseStack.pushPose();

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

        float lineAlpha = ((isPlanting && ghostPlantState != null) || (isHoe && FarmingConfig.GHOST_FARMLAND_PREVIEW.get())) ? 0.35f : a;
        boolean isCocoaPlanting = isPlanting && FarmingManager.isCocoaBean(activeSeed);
        Set<BlockPos> waterHoleSet = plannedWaterHoles.isEmpty() ? Collections.emptySet() : new HashSet<>(plannedWaterHoles);
        for (BlockPos pos : previewBlocks) {
            BlockPos renderPos = (isPlanting && !isCocoaPlanting) ? pos.above() : pos;
            net.minecraft.world.phys.AABB aabb;
            if (isPlanting) {
                aabb = new net.minecraft.world.phys.AABB(renderPos);
            } else {
                BlockState state = level.getBlockState(renderPos);
                VoxelShape shape = state.getShape(level, renderPos);
                aabb = shape.isEmpty()
                        ? new net.minecraft.world.phys.AABB(renderPos)
                        : shape.bounds().move(renderPos);
            }
            net.minecraft.world.phys.AABB camRelative = aabb.move(-camPos.x, -camPos.y, -camPos.z);

            float boxR = r, boxG = g, boxB = b;
            float currentAlpha = lineAlpha;
            if (isHoe && FarmingConfig.SMART_IRRIGATION_PREVIEW.get()) {
                if (waterHoleSet.contains(pos)) {
                    boxR = 0.15f; boxG = 0.65f; boxB = 1.0f; // Azure Water Blue
                    currentAlpha = 0.85f;
                } else {
                    boolean hasWater = FarmingManager.isNearWater(level, pos) || FarmingManager.isHydratedByHoles(pos, plannedWaterHoles);
                    if (hasWater) {
                        boxR = 0.45f; boxG = 0.65f; boxB = 0.35f; // Hydrated Farmland Green-Brown
                        currentAlpha = 0.40f;
                    } else {
                        boxR = 0.95f; boxG = 0.45f; boxB = 0.15f; // Warning Dry Amber/Orange
                        currentAlpha = 0.65f;
                    }
                }
            } else if (isPlanting && plannedIntercrop != null && FarmingConfig.GROWTH_PENALTY_WARNING.get()) {
                Block plannedCrop = plannedIntercrop.get(renderPos);
                boolean hasPenalty = FarmingManager.hasGrowthPenalty(level, renderPos, plannedCrop, plannedIntercrop);
                if (hasPenalty) {
                    boxR = 1.0f; boxG = 0.28f; boxB = 0.2f; // Warning Coral Crimson
                    currentAlpha = 0.85f;
                } else {
                    boxR = 0.35f; boxG = 0.85f; boxB = 0.3f; // Full 200% growth speed Sprout Green
                    currentAlpha = 0.35f;
                }
            }

            renderLineBox(poseStack, consumer, camRelative, boxR, boxG, boxB, currentAlpha);
        }

        poseStack.popPose();
        bufferSource.endBatch(RenderTypes.lines());

        // Render translucent 3D ghost preview for Farmland (and planned Water Holes) when holding a hoe
        if (isHoe && FarmingConfig.GHOST_FARMLAND_PREVIEW.get() && !previewBlocks.isEmpty()) {
            VertexConsumer quadConsumer = bufferSource.getBuffer(RenderTypes.debugQuads());
            poseStack.pushPose();
            for (BlockPos pos : previewBlocks) {
                BlockState targetState = level.getBlockState(pos);
                VoxelShape shape = targetState.getShape(level, pos);
                double blockTop = shape.isEmpty() ? 1.0 : shape.max(Direction.Axis.Y);

                if (waterHoleSet.contains(pos)) {
                    double topY = pos.getY() + blockTop + 0.003;
                    net.minecraft.world.phys.AABB waterBox = new net.minecraft.world.phys.AABB(
                            pos.getX() - 0.001, pos.getY(), pos.getZ() - 0.001,
                            pos.getX() + 1.001, topY, pos.getZ() + 1.001
                    ).move(-camPos.x, -camPos.y, -camPos.z);
                    renderFilledBox(poseStack, quadConsumer, waterBox, 0.15f, 0.65f, 1.0f, 0.40f);
                } else {
                    boolean hasWater = FarmingManager.isNearWater(level, pos) || FarmingManager.isHydratedByHoles(pos, plannedWaterHoles);
                    double topY = pos.getY() + blockTop + 0.003;
                    net.minecraft.world.phys.AABB farmlandBox = new net.minecraft.world.phys.AABB(
                            pos.getX() - 0.001, topY - 0.9375, pos.getZ() - 0.001,
                            pos.getX() + 1.001, topY, pos.getZ() + 1.001
                    ).move(-camPos.x, -camPos.y, -camPos.z);
                    if (hasWater) {
                        renderFilledBox(poseStack, quadConsumer, farmlandBox, 0.30f, 0.20f, 0.12f, 0.38f);
                    } else {
                        renderFilledBox(poseStack, quadConsumer, farmlandBox, 0.60f, 0.40f, 0.22f, 0.30f);
                    }
                }
            }
            poseStack.popPose();
            bufferSource.endBatch(RenderTypes.debugQuads());
        }

        // Render translucent 3D ghost preview for planted crops/saplings
        if (ghostPlantState != null && !previewBlocks.isEmpty()) {
            VertexConsumer quadConsumer = bufferSource.getBuffer(RenderTypes.debugQuads());
            BlockPos originSoilPos = FarmingManager.isValidSoilForSeed(activeSeed, clickedState, level, clickedPos) ? clickedPos : clickedPos.below();
            Block mainCropBlock = FarmingManager.getCropBlock(activeSeed);
            Block offCropBlock = FarmingManager.getCropBlock(offItem);

            boolean evenIsMain = true;
            if (ghostOffState != null) {
                evenIsMain = FarmingManager.determineIntercropPhase(level, originSoilPos, player.getDirection(), mainCropBlock, offCropBlock);
            }
            boolean alternateOnX = (player.getDirection().getAxis() == Direction.Axis.Z);
            poseStack.pushPose();
            for (BlockPos pos : previewBlocks) {
                boolean isMainRow = true;
                if (!isCocoaPlanting && ghostOffState != null) {
                    int rowCoord = alternateOnX ? (pos.getX() - originSoilPos.getX()) : (pos.getZ() - originSoilPos.getZ());
                    isMainRow = (Math.floorMod(rowCoord, 2) == 0) ? evenIsMain : !evenIsMain;
                }
                BlockPos plantPos = isCocoaPlanting ? pos : pos.above();
                net.minecraft.world.phys.AABB plantBox = new net.minecraft.world.phys.AABB(
                        plantPos.getX() + 0.15, plantPos.getY() + 0.02, plantPos.getZ() + 0.15,
                        plantPos.getX() + 0.85, plantPos.getY() + 0.65, plantPos.getZ() + 0.85
                ).move(-camPos.x, -camPos.y, -camPos.z);
                if (isMainRow) {
                    renderFilledBox(poseStack, quadConsumer, plantBox, 0.35f, 0.85f, 0.30f, 0.28f);
                } else {
                    renderFilledBox(poseStack, quadConsumer, plantBox, 0.90f, 0.80f, 0.25f, 0.28f);
                }
            }
            poseStack.popPose();
            bufferSource.endBatch(RenderTypes.debugQuads());
        }

        // Render translucent 3D ghost fruits (Melon / Pumpkin) on the reserved fruit spots
        if (isPlanting && FarmingManager.isFruitSeed(activeSeed) && !previewBlocks.isEmpty()) {
            Block fruitBlock = FarmingManager.getFruitForSeed(activeSeed);
            if (fruitBlock != null) {
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

                if (!fruitSpots.isEmpty()) {
                    VertexConsumer quadConsumer = bufferSource.getBuffer(RenderTypes.debugQuads());
                    boolean isMelon = fruitBlock == Blocks.MELON;
                    float fr = isMelon ? 0.50f : 0.90f;
                    float fg = isMelon ? 0.80f : 0.52f;
                    float fb = isMelon ? 0.25f : 0.15f;
                    poseStack.pushPose();
                    for (BlockPos fruitPos : fruitSpots) {
                        net.minecraft.world.phys.AABB fruitBox = new net.minecraft.world.phys.AABB(
                                fruitPos.getX() + 0.05, fruitPos.getY() + 0.05, fruitPos.getZ() + 0.05,
                                fruitPos.getX() + 0.95, fruitPos.getY() + 0.95, fruitPos.getZ() + 0.95
                        ).move(-camPos.x, -camPos.y, -camPos.z);
                        renderFilledBox(poseStack, quadConsumer, fruitBox, fr, fg, fb, 0.28f);
                    }
                    poseStack.popPose();
                    bufferSource.endBatch(RenderTypes.debugQuads());
                }
            }
        }
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

    private static void renderLineBox(PoseStack poseStack, VertexConsumer consumer, net.minecraft.world.phys.AABB box, float r, float g, float b, float a) {
        org.joml.Matrix4f matrix = poseStack.last().pose();
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        // Bottom 4 edges
        drawBoxLine(consumer, matrix, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        drawBoxLine(consumer, matrix, minX, minY, minZ, minX, minY, maxZ, r, g, b, a);
        drawBoxLine(consumer, matrix, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        drawBoxLine(consumer, matrix, minX, minY, maxZ, maxX, minY, maxZ, r, g, b, a);

        // Top 4 edges
        drawBoxLine(consumer, matrix, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        drawBoxLine(consumer, matrix, minX, maxY, minZ, minX, maxY, maxZ, r, g, b, a);
        drawBoxLine(consumer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        drawBoxLine(consumer, matrix, minX, maxY, maxZ, maxX, maxY, maxZ, r, g, b, a);

        // 4 Vertical edges
        drawBoxLine(consumer, matrix, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        drawBoxLine(consumer, matrix, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        drawBoxLine(consumer, matrix, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
        drawBoxLine(consumer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
    }

    private static void drawBoxLine(VertexConsumer consumer, org.joml.Matrix4f matrix,
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
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setNormal(dx, dy, dz).setLineWidth(1.0F);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setNormal(dx, dy, dz).setLineWidth(1.0F);
    }

    private static void renderFilledBox(PoseStack poseStack, VertexConsumer consumer, net.minecraft.world.phys.AABB box,
                                        float r, float g, float b, float a) {
        org.joml.Matrix4f matrix = poseStack.last().pose();
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        // Bottom (Y-)
        consumer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, a);
        consumer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, a);
        consumer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, a);
        consumer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, a);

        // Top (Y+)
        consumer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, a);
        consumer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, a);
        consumer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, a);
        consumer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, a);

        // North (Z-)
        consumer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, a);
        consumer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, a);
        consumer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, a);
        consumer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, a);

        // South (Z+)
        consumer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, a);
        consumer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, a);
        consumer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, a);
        consumer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, a);

        // West (X-)
        consumer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, a);
        consumer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, a);
        consumer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, a);
        consumer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, a);

        // East (X+)
        consumer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, a);
        consumer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, a);
        consumer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, a);
        consumer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, a);
    }
}
