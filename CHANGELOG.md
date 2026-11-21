# Changelog

All notable changes to **Vein Farming** are documented in this file.

---

## [2.2.0] — Minecraft 26.x Ports & Visual Overhaul

- **NeoForge 1.21.11 & 26.x Ports:** Added full support for NeoForge 1.21.11 alongside Minecraft 26.1, 26.2, and 26.3 on Fabric and NeoForge (12 ports total).
- **Real 3D Ghost Models:** Replaced debug cubes with real textured, tinted 3D block models for tilling, water holes, seeds, and fruits.
- **Water & Ice Transparency Fix:** Switched ghost model rendering to `beaconBeam` (`depthWrite = false`), eliminating X-ray occlusion over water, ice, and stained glass.
- **Camera Occlusion Skip:** Automatically skips rendering ghost block models when the camera stands inside a preview block to prevent first-person view obstruction.
- **Anti-Flicker & Z-Fighting Fix:** Re-enabled Backface Culling, batched geometry into a single submit pass, and fine-tuned offsets (+0.004/+0.006) to eliminate flickering.
- **Cover Layer Offset:** Preview wireframes and ghost blocks automatically elevate above low surface covers (snow layers, leaf litter, petals).
- **Block Outline Fix:** Preserved vanilla block selection outline when holding tools/seeds near non-farm blocks; added destroy outline fallback for 1-block column crops (cactus, sugar cane, bamboo).
- **Smart Enclosed Water Holes:** Improved irrigation algorithm (`isWaterHoleContained`) to require solid surrounding walls, preventing water spills on slopes, cliffs, and terraces.
- **NeoForge Fluid API:** Integrated NeoForge Transfer API for fluid container interoperability.

---

## [2.1.0] — Multi-Loader Architecture & Fabric 1.20.1

- Refactored project into shared per-version `common/` modules with `PlatformBridge`.
- Added full Fabric 1.20.1 port alongside Forge 1.20.1, NeoForge 1.21.1, Fabric 1.21.1, and Fabric 1.21.11.
- Updated GitHub Actions CI and build scripts to support all 5 ports.

---

## [2.0.3] — Bug Fixes & Refinements

- Added claim protection across all mass farming actions.
- Improved water irrigation algorithm to prevent spills on slopes and terraces.
- Fixed stuck activation key detection and excluded non-tillable blocks.
- Optimized 2x2 sapling planting and melon/pumpkin layout near water.
- Enhanced performance with BFS chunk safety and fast-path rendering.
- Cleaned metadata and added multi-loader GitHub Actions CI.

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

