# Changelog

All notable changes to **Vein Farming** are documented in this file.

---

## [2.0.3] — Claim Protection, Performance & Algorithm Fixes

- Enforced per-block claim protection (`BlockEvent.BreakEvent` / `PlayerBlockBreakEvents.BEFORE`) across mass harvest, destroy, planting, tilling, water-hole digging, and flower bone meal.
- Upgraded Smart Water Bucket Irrigation (`PlantingAlgorithms.isWaterHoleContained`) to require a solid floor and 4 enclosing horizontal blocks so water never spills off edges, terraces, or slopes.
- Fixed Standalone outline preview getting stuck active by polling live GLFW physical key state, synced client-server activation rules, and excluded non-tillable blocks (`moss`, `mud`, `podzol`, `mycelium`) from hoe preview.
- Fixed melon/pumpkin stem parity near water (requiring at least one open adjacent fruit spot), added upper-block clearance and claim checks for 2-tall flowers (`DoublePlantBlock`), and enforced complete 4-sapling batching for strictly 2×2 saplings (Dark Oak / Pale Oak).
- Added `level.isLoaded` guards to all BFS scans, optimized server farmland fall checks (`MutableBlockPos` + `FARMLAND_CACHE`), and added early fast-path exit in `StandaloneHighlightRenderer`.
- Filtered decorative/storage false positives in `PlantClassifier`, unified NeoForge `LiteMinerCompat` on reflection (removed `libs/liteminer.jar`), updated metadata to `Velorise`/`velorise2206`, and added multi-loader GitHub Actions CI.

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
