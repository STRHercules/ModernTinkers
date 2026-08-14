## 2026-08-13

- Added mandatory task-log and `Docs/TASK.md` review requirements to `AGENTS.md`.
- Set the current project version to `1.2.3` and standardized the artifact name as `ModernTinkers-1.21.1-1.2.3-NeoForge.jar`.
- Added the rule that every cohesive repository change increments the rightmost build/version component.

## 2026-08-13 — Initial Tinkers material-item port

- Began the port with the TinkerMaterials ingredient subset: copper nugget,
  netherite nugget, debris scrap, necrotic bone, venombone, blazing bone, and
  necronium bone.
- Added NeoForge deferred registration, a temporary general creative tab,
  English names/tooltips, item models, reference textures, and common/local
  item tags.
- Advanced the project version to 1.2.4.

## 2026-08-13 — Static Tinkers registry sweep

- Added the first static material registry sweep: thirteen metal
  block/ingot/nugget sets plus Nahuatl, Blazewood, and the storage placeholder.
- Added the reference tool-part IDs, pattern, modifier-material items, and
  modifier crystal, each with inventory presentation and focused creative tabs.
- Added Crafting Station, Tinker Station, Part Builder, Modifier Worktable,
  Tinkers' Anvil, and Scorched Anvil block shells with authored resources.
- Corrected blockstate model identifiers and advanced the project version to
  1.2.5.

## 2026-08-13 — Table acquisition and crafting slice

- Made the ModernTinkers Crafting Station use vanilla's server-safe crafting
  table interaction while preserving its registered block ID and assets.
- Added reference-shaped recipes for patterns, Crafting Station, Part Builder,
  and Tinker Station acquisition.
- Added all 13 metal block/ingot/nugget conversion families and a local pattern
  tag, giving the registered static materials a vanilla crafting path.
- Advanced the project version to 1.2.6.

## 2026-08-13 — Part Builder logic and systems slice

- Replaced the static Part Builder block with a persistent custom block entity
  and registered menu that owns pattern/material inputs and drops them safely
  when the block is broken.
- Added sneak-use pattern selection through `CUSTOM_DATA`, server-derived
  material-aware tool-part results, nine-nugget material units, output-pickup
  consumption, persistence, and shift-click routing.
- Advanced the project version to 1.2.7.

## 2026-08-13 — Large systems and indexed-tool sweep

- Added the first consolidated material, fluid, smeltery, table, modifier,
  tool, armor, shield, arrow, and world-content systems under `src/`.
- Added server-authoritative Tinker Station, Modifier Worktable, casting,
  melter, alloyer, and smeltery-controller paths, plus indexed multi-part tool
  assembly and material-aware melting.
- Added the remaining registered tool IDs and initial special-tool area
  behavior, then advanced the project version to 1.2.8.
