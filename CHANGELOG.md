# Changelog

All notable changes to **Vein Farming** are documented in this file.

---

## [2.0.3] — Claim Protection, Performance & Cleanup

- Enforced per-block claim protection (`BlockEvent.BreakEvent` / `PlayerBlockBreakEvents.BEFORE`) across mass harvest, destroy, planting, tilling, water-hole digging, and flower bone meal.
- Added chunk-loaded guards (`level.isLoaded`) to all BFS scans to prevent synchronous chunk loading at chunk borders.
- Optimized server farmland fall-cushion checks with `MutableBlockPos`, downward-velocity filtering, and cached `PlantClassifier.isFarmland`.
- Added early fast-path exit in `StandaloneHighlightRenderer` when the activation key is not held.
- Fixed stuck/false activation preview in Standalone mode by polling live GLFW key state (`ModKeyMappings.isKeyActive`), synced companion-key gating between client and server, and excluded non-tillable blocks (moss, mud, podzol, mycelium) from hoe preview.
- Filtered decorative/storage false positives (crates, pots, lamps, logs, planks, carved pumpkins) and cached fallback lookups in `PlantClassifier`.
- Upgraded Smart Water Bucket Irrigation (`PlantingAlgorithms.isWaterHoleContained`) to require solid enclosure on all 4 horizontal sides plus floor, preventing water holes from spilling off edges, slopes, or non-solid blocks.
- Unified NeoForge 1.21.1 `LiteMinerCompat` on reflection, removed bundled `libs/liteminer.jar`, updated mod metadata to `Velorise`/`velorise2206`, and added multi-loader GitHub Actions CI.

---

## [2.0.2] — Bug Fixes & Refinements

- Added offhand seed selection support for ghost preview.
- Disallowed tilling grass foliage and avoided creating water holes on grass blocks.
- Added claim protection integration and reflection caching.
- Enhanced tool break safety to prevent breaking hoes during mass actions.
- Supported vertical sugar cane, bamboo, and cactus column harvesting with root preservation.

---

## [2.0.1] — Multi-Loader Expansion

- Ported Vein Farming to Minecraft Forge 1.20.1.
- Synchronized features across Forge 1.20.1, NeoForge 1.21.1, Fabric 1.21.1, and Fabric 1.21.11.
- Clarified activation key priority across FTB Ultimine, LiteMiner, and Standalone mode.

---

## [2.0.0] — Initial Multi-Loader Release

- Universal AOE farming toolkit for NeoForge 1.21.1, Fabric 1.21.1, and Fabric 1.21.11.
- Built-in Standalone mode with client-side in-world highlight outline preview.
- 2-Pass Hybrid Irrigation Grid for optimal water hole placement with 100% hydration guarantee.
- Smart mass planting with spatial geometry layouts (cross, checkerboard, spacing filters).
- Mass tilling, AOE bone meal, stem-protected fruit harvesting, and batch composting.

