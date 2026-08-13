# ModernTinkers Agent Guide

## Project mission

ModernTinkers is a single-bundle port and consolidation of Tinkers' Construct and selected companion mods for NeoForge on Minecraft 1.21.1.

The goal is a coherent modern implementation, not a collection of independently shipped legacy mods. Internal packages or modules may be used for ownership and maintainability, but the intended player-facing result is one bundle with consistent registration, configuration, resources, and compatibility behavior.

The repository now has an initial Gradle/NeoForge bootstrap. Feature implementation is still in the reference and planning phase: legacy source material is present under `References/`, while all new implementation belongs under `src/`.

## Non-negotiable source boundary

`References/` is archival, read-only input material.

Agents may search, inspect, compare, and learn from it, but must not:

- edit, format, rename, move, or delete files under `References/`;
- write generated sources, build output, caches, IDE metadata, or temporary files there;
- update old Gradle projects in place to make them build;
- treat a legacy project in `References/` as the active implementation; or
- silently patch a reference file instead of adapting the behavior in `src/`.

If reference behavior needs to change for NeoForge 1.21.1, implement that change in the new project under `src/` and document the relevant source and intentional differences. Preserve upstream copyright, license, and attribution requirements before reusing code, assets, names, or data.

New Java/Kotlin code, resources, tests, and port-specific data must live under `src/`. Root-level build configuration and documentation may be added when needed to support that implementation; they do not change the read-only status of `References/`.

## Current reference inventory

The current contents of `References/` include:

- `TinkersConstruct-1.20.1/` — the closest modern Tinkers' Construct behavior and API reference.
- `Mantle-1.20/` — shared-library and support-code reference.
- `constructsarmory-master/` — Construct's Armory content and behavior reference.
- `MoarTinkers-master/` — additional Tinkers content and material reference.
- `Oreberries-1.12.1/` — legacy oreberry content and behavior reference.
- `TiC-Tooltips-1.7.10/` — legacy tooltip behavior reference.
- `TinkersJEI-master/` — recipe-viewer integration reference.
- `TinkersToolLeveling-master/` — tool-leveling behavior reference.

Verify behavior and licensing in the specific reference project before relying on it. Do not assume that an older Forge API, registry name, data format, client hook, or dependency still applies unchanged to NeoForge 1.21.1.

## Implementation guidance

### Target platform

- Target NeoForge for Minecraft 1.21.1.
- Use the Java version required by that target; Minecraft 1.21.1 development is expected to use Java 21.
- Prefer current NeoForge APIs and vanilla data systems over compatibility shims for obsolete Forge APIs.
- Keep dedicated-server safety in mind: client-only classes and registration must not load on the server.
- Keep the bundle's public IDs, namespaces, configuration keys, and data formats deliberate and stable once released.

### Porting approach

Before implementing a feature:

1. Identify the feature's authoritative reference source and read its license and attribution requirements.
2. Trace the complete behavior, including registration, serialization, recipes, loot, tags, events, client presentation, and integrations.
3. Record what is being preserved, what must change for 1.21.1, and what is intentionally omitted.
4. Implement the modern behavior in the appropriate `src/` package or resource path.
5. Add focused automated coverage where practical and list any gameplay or visual checks that still require a client/server smoke test.

Do not mechanically merge old projects or copy their obsolete build scaffolding. Port cohesive behavior and keep one clear owner for each registry entry, event hook, data definition, and integration.

Prefer data-driven content for recipes, loot tables, tags, tool definitions, materials, modifiers, translations, models, and other content that NeoForge and vanilla support through data or resources. Keep gameplay rules in code when they cannot be expressed safely as data.

When combining companion content, resolve collisions intentionally:

- use one canonical registration for shared concepts;
- preserve compatible player-facing behavior unless the port documents a difference;
- avoid duplicate items, blocks, fluids, recipes, events, or tool systems;
- keep optional integrations isolated and safe when the other mod is absent; and
- do not add unrelated features merely because they exist in a reference project.

## Versioning and artifact naming

`gradle.properties` is the source of truth for `mod_version`. The current project version is `1.2.3`.

Every repository change, including documentation, configuration, and new files that will become part of the project, must increment the build number before completion. For ordinary changes, increment the rightmost version component: `1.2.3` becomes `1.2.4`. Never reuse a version. A single cohesive task that changes multiple files receives one version bump. Only use a major or minor-version change when the user explicitly requests one; otherwise the rightmost component is the build number.

Every build must produce this exact filename pattern:

```text
build/libs/ModernTinkers-1.21.1-<current-version>-NeoForge.jar
```

With the current version, the expected artifact is `ModernTinkers-1.21.1-1.2.3-NeoForge.jar`. The artifact base name, Minecraft version, current version, and `NeoForge` suffix are intentional and must not revert to Gradle's default `moderntinkers-<version>.jar` naming. Keep the artifact name in `build.gradle` and the version in `gradle.properties` synchronized.

Before handoff, run a fresh build after the version bump and verify that exactly the current-version artifact exists in `build/libs/`. Do not claim completion from an older or differently named JAR.

## Repository layout

Use this ownership model as the repository grows:

```text
ModernTinkers/
├── AGENTS.md                         # Instructions for agents and contributors
├── README.md                         # Project overview and current status
├── build.gradle                      # NeoForge ModDevGradle build
├── gradle.properties                 # Target and mod metadata
├── gradle/wrapper/                   # Checked-in Gradle Wrapper
├── References/                       # Legacy source material; read-only
└── src/                              # New NeoForge 1.21.1 implementation
    ├── main/
    │   ├── java/         # Java implementation
    │   └── resources/    # Mod metadata, data, assets, and translations
    └── test/             # Automated tests and test resources
```

If the eventual Gradle layout differs, update the documentation to reflect the actual layout rather than creating parallel conventions.

## Validation

The repository contains an initial build scaffold using Java 21, Gradle Wrapper 9.2.1, ModDevGradle 2.0.143, and NeoForge 21.1.240 for Minecraft 1.21.1. Keep these versions in sync with `gradle.properties` and the wrapper configuration when changing the target.

Once the NeoForge project is scaffolded, prefer the repository's Gradle wrapper:

```text
gradlew.bat clean build
gradlew.bat test
gradlew.bat runClient
```

For every completed repository change, increment `mod_version` and run `gradlew.bat clean build --console=plain --no-daemon`. Use only tasks that the project actually defines. Build and unit-test success is not sufficient evidence for registration, client rendering, recipe discovery, multiplayer behavior, or compatibility.

For a meaningful port milestone, validate as applicable:

- the mod loads on both client and dedicated server;
- registries, datagen, tags, recipes, loot, translations, models, and textures resolve;
- the core Tinkers' Construct progression and tool interactions work in a fresh test world;
- ported companion content does not create duplicate or conflicting behavior;
- JEI or other optional integrations remain safe when unavailable and functional when present; and
- multiplayer/server-authority behavior is tested for state that leaves the player or world.

Separate automated evidence from manual gameplay, visual, and multiplayer checks in status reports.

## Change and review hygiene

- Check `git status` before editing and preserve unrelated user changes.
- Read `Docs/TASK.md` before starting every task. Treat active instructions and task boundaries there as authoritative; if it is empty, proceed using this file and the user's request without inventing task scope.
- Keep changes focused on the requested port slice.
- Never stage or commit changes from `References/` as implementation edits.
- Review the final diff and verify that generated output, IDE files, and local caches are not included.
- Keep source provenance and license notices close to imported or substantially adapted material.
- Update `README.md` when the project status, supported content, build workflow, or ownership boundary changes.

## Mandatory project log updates

Every completed task or change, including code, configuration, build, documentation, and repository-maintenance work, must append one new dated entry to each of these files:

- `Docs/CHANGELOG.md` — concise summary of what changed and the player or repository-facing result.
- `Docs/TRACELOG.md` — implementation evidence, checks run, relevant paths, and any known gaps.
- `Docs/SUGGESTIONS.md` — one actionable follow-up, or an explicit `No new suggestions` entry when no follow-up is warranted.

Append to the existing files; do not replace or rewrite earlier entries. Match an established format when one exists. If a log is empty, use a compact dated Markdown entry with the task summary. Do not claim a build, gameplay check, visual check, or server check that was not actually performed. Before handing work back, verify that all three entries exist and mention any pending validation or follow-up.

Definition of done for a ported feature:

1. The implementation and resources are under `src/`.
2. Reference behavior, target-version adaptations, and intentional differences are understood and documented where useful.
3. Automated validation passes at the appropriate level.
4. Required client, dedicated-server, and gameplay smoke checks are complete or explicitly recorded as pending.
5. The final diff contains no edits to `References/`.
