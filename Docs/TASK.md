# Current port milestone

## Core systems and parity sweep — 2026-08-13

Continue the Tinkers' Construct 1.20.1 behavior port on NeoForge 1.21.1 in
cohesive player-facing systems. Prioritize the registered content already
under `src/`: complete tool assembly and modifier transactions, close the
smeltery/casting/fluid persistence paths, implement special tools and armor/
shield behavior, and add missing data-driven recipes, loot, tags, models, and
world acquisition. Keep `References/` read-only and document intentional 1.21.1
adaptations.

Acceptance checks:

- Tool, armor, shield, projectile, and table operations remain server-
  authoritative and validate indexed parts/materials/modifiers before writes.
- Modifier application, removal/repair, durability, traits, and relevant event
  hooks have a working path for the registered modifier set.
- Melter, alloyer, casting, tank, faucet, heater, and smeltery controller state
  persists and transfers fluids/results transactionally.
- Registered special tools have their core area interactions and combat/use
  behavior, with safe guards for empty or legacy stacks.
- Resource/data audits find no dangling authored model, recipe, loot, tag, or
  translation references, and `References/` has no changes.
- Bump the rightmost version component, run `gradlew.bat clean build
  --console=plain --no-daemon`, and verify exactly one current-version JAR.

Known final validation boundary:

- Client rendering, dedicated-server startup, fresh-world gameplay,
  multiplayer synchronization, and optional integrations require runtime smoke
  checks in addition to compilation and static audits.
