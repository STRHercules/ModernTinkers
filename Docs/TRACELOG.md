## 2026-08-13

- Checked `Docs/TASK.md` before editing; it was empty.
- Updated `AGENTS.md` to require reading `Docs/TASK.md` before every task and appending a dated entry to `Docs/CHANGELOG.md`, `Docs/TRACELOG.md`, and `Docs/SUGGESTIONS.md` for every completed change.
- Validation: documentation-only change; `git diff --check` run after the edit.
- Updated `gradle.properties`, `build.gradle`, `README.md`, and the project logs for version `1.2.3` and the required `ModernTinkers-1.21.1-1.2.3-NeoForge.jar` output. The first clean build exposed Gradle's automatic version suffix (`...-NeoForge-1.2.3.jar`); `Jar.archiveFileName` now sets the exact requested filename.
- Validation passed: `gradlew.bat clean build --console=plain --no-daemon`; exactly one artifact was present in `build/libs/`, named `ModernTinkers-1.21.1-1.2.3-NeoForge.jar`; the archive contained `com/moderntinkers/ModernTinkers.class`, `META-INF/neoforge.mods.toml`, and `pack.mcmeta`; SHA-256 was `63fde0ceb9ba400f1cc0f3e409012646c3ce8c8237d2835d69fda32c92243cfd`.

## 2026-08-13 — Initial Tinkers material-item port

- Reference inspected: References/TinkersConstruct-1.20.1, specifically
  TinkerMaterials, its English material-item translations/models, item tags,
  and the MIT license.
- Implemented the first slice in src/main/java/com/moderntinkers/content/
  MaterialItems.java and registered it from ModernTinkers.java. Added authored
  models, English strings, common/local tags, and copied only the seven
  required texture assets into the moderntinkers namespace.
- Intentional target adaptation: the legacy Mantle item-layer/emissive model
  for blazing bone is represented by the vanilla bone parent until that
  renderer is ported. All seven items are temporarily exposed in the general
  tab until the reference world/material tabs exist.
- Checks passed: rtk git diff --check; JSON parsing of all authored resource
  files; gradlew.bat clean build --console=plain --no-daemon with BUILD
  SUCCESSFUL and test NO-SOURCE; exactly one build artifact exists,
  ModernTinkers-1.21.1-1.2.4-NeoForge.jar, containing the new class, models,
  textures, tags, and META-INF/neoforge.mods.toml.
- Pending manual validation: client creative-tab/model/tooltip rendering,
  dedicated-server startup, and gameplay acquisition through smeltery recipes
  and world drops. No files under References were modified.

## 2026-08-13 — Static Tinkers registry sweep

- Reference inspected: `References/TinkersConstruct-1.20.1`, specifically the
  `TinkerMaterials`, `TinkerToolParts`, `TinkerModifiers`, `TinkerTables`, and
  `TinkerCommons` registration/resource paths, plus the reference MIT license.
- Implemented the sweep in `src/main/java/com/moderntinkers/content/StaticContent.java`
  and registered it from `ModernTinkers.java`. Added 13 metal storage/ingot/
  nugget sets, Nahuatl, Blazewood, the storage placeholder, 29 tool-part IDs,
  pattern, modifier-material items, modifier crystal, and six table/anvil
  block shells. Added focused creative tabs, English strings, authored model
  and blockstate definitions, selected reference textures, and tags under
  `src/main/resources/`.
- Intentional target adaptation: these are static IDs and inventory-visible
  placeholders only. Material serialization, tool assembly, modifier
  behavior, block entities/menus, recipes, smeltery fluids, worldgen, loot,
  dynamic variants, and optional integrations remain deferred. Table models
  use simple port-authored geometry until the full reference table rendering
  path is ported.
- Checks passed: Python JSON parsing for all 191 resource JSON files; 44
  ModernTinkers model references resolved; no duplicated namespace references;
  `git diff --check`; no status changes under `References/`; clean
  `gradlew.bat clean build --console=plain --no-daemon` with `BUILD
  SUCCESSFUL` and `test NO-SOURCE`.
- Exactly one artifact exists: `build/libs/ModernTinkers-1.21.1-1.2.5-NeoForge.jar`.
  The packaged archive contains the new classes, NeoForge metadata, 232
  ModernTinkers asset entries, and 8 data entries. SHA-256:
  `f78a5c5a30f6544df67e27492cb2ba1638b3431f807f0908d27cddf45c5d5361`.
- Pending manual validation: client creative-tab and model rendering,
  dedicated-server startup, table interaction, recipes, smeltery/world
  acquisition, and gameplay/progression checks. No files under `References/`
  were modified.

## 2026-08-13 — Table acquisition and Crafting Station slice

- Reference inspected: `References/TinkersConstruct-1.20.1`, especially
  `TinkerTables` and `TableRecipeProvider`. The port keeps the reference
  pattern/table shapes where the current bundle has equivalent inputs, and
  adapts the reference wooden-rod ingredient to vanilla `minecraft:stick`.
- Updated `StaticContent.java` so `crafting_station` constructs a vanilla
  `CraftingTableBlock`; this gives the registered ModernTinkers block a real,
  server-safe crafting menu without loading client-only classes.
- Added 56 recipe JSON files under
  `src/main/resources/data/moderntinkers/recipes/`: pattern, Crafting Station,
  Part Builder, Tinker Station, and four conversion recipes for each of the 13
  registered metal sets. Added the local `patterns` item tag.
- Intentional target boundary: the Part Builder, Tinker Station, Modifier
  Worktable, and both anvil IDs remain static blocks. Their custom block
  entities, menus, material-aware recipes, and progression rules are not
  claimed by this slice. The metal conversions are vanilla datapack recipes
  over existing `c:` tags, not a replacement for Tinkers' material registry or
  smeltery alloy logic.
- Checks passed: `gradlew.bat compileJava --console=plain --no-daemon`; clean
  `gradlew.bat clean build --console=plain --no-daemon` with `BUILD SUCCESSFUL`
  and `test NO-SOURCE`; Python parsing of all 248 JSON resources; 44 authored
  ModernTinkers model references resolved; `git diff --check`; no status
  changes under `References/`.
- Exactly one artifact exists:
  `build/libs/ModernTinkers-1.21.1-1.2.6-NeoForge.jar`. The archive contains
  365 entries, 232 ModernTinkers asset entries, 120 data entries, and all 56
  recipe files. SHA-256:
  `8c6a21b6d781b84fa6dbd99153cdf3c02821cabafa526d5502fc29d8932f951c`.
- Pending manual validation: client/server startup, Crafting Station GUI
  interaction, recipe discovery and output in a fresh world, visual model
  checks, and gameplay/progression checks for the still-static custom tables.
  No files under `References/` were modified.

## 2026-08-13 — Part Builder logic and systems slice

- Read `Docs/TASK.md` before editing and inspected the reference table/entity,
  menu, pattern, and item-part recipe paths under
  `References/TinkersConstruct-1.20.1/`.
- Implemented `PartBuilderBlock`, `PartBuilderBlockEntity`, `PartBuilderMenu`,
  and `PatternItem` under `src/main/java/com/moderntinkers/content/partbuilder/`.
  `StaticContent` now registers the custom block, block entity, and menu while
  preserving the existing registry IDs and resources.
- The block entity persists its two inputs with the 1.21.1 holder lookup
  provider, derives output server-side, maps current ModernTinkers and vanilla
  ingot/nugget inputs to nine-unit material values, stores
  `moderntinkers:material` on output stacks, consumes inputs on pickup, and
  drops inputs on block removal. Patterns remain reusable and cycle their
  `moderntinkers:part` selection on sneak-use.
- Checks passed: `gradlew.bat compileJava --console=plain --no-daemon`; clean
  `gradlew.bat clean build --console=plain --no-daemon`; `git diff --check`;
  248 authored JSON resources and 56 recipes remained present; and no status
  changes were reported under `References/`.
- Exactly one artifact exists:
  `build/libs/ModernTinkers-1.21.1-1.2.7-NeoForge.jar`. The archive contains
  the Part Builder block/entity/menu classes, `PatternItem`, NeoForge metadata,
  and the updated translations. SHA-256:
  `64d71b2baf600e22c93dfda39fbf445cf2de09f61c73e8da752e0aebce689fd1`.
- Pending validation: client screen registration, client/server startup,
  fresh-world menu interaction, output rendering, and multiplayer/runtime
  behavior remain manual follow-ups. No files under `References/` were
  modified.

## 2026-08-13 — Large systems and indexed-tool sweep

- Continued from the Part Builder slice using the read-only Tinkers' Construct
  reference paths for tool definitions, part layouts, table logic, smeltery
  registrations, and tool events.
- Implemented the current `src/` systems for materials, fluids, smeltery
  machines, casting, tables, modifiers, armor, shields, arrows, tools, and
  initial world content. Tool stacks now retain indexed `moderntinkers:parts`
  data while continuing to read the legacy head/handle/binding shape; Tinker
  Station assembly and smeltery melting use all indexed parts.
- Added remaining standard and ancient tool IDs and bounded special-tool
  interactions, including vein breaking and minotaur-axe log columns. No
  files under `References/` were modified.
- Validation passed: `gradlew.bat clean build --console=plain --no-daemon`
  with `BUILD SUCCESSFUL`, `git diff --check`, and exactly one artifact at
  `build/libs/ModernTinkers-1.21.1-1.2.8-NeoForge.jar`.
- Pending validation: dedicated-server/client loading, fresh-world table and
  smeltery transactions, visual tool/armor rendering, multiplayer state, full
  modifier runtime behavior, and optional integrations.
