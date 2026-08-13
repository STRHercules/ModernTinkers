# ModernTinkers

ModernTinkers is a single NeoForge bundle for Minecraft 1.21.1 that ports and consolidates Tinkers' Construct and selected companion mods from older Minecraft and Forge versions.

## Status

The repository now has a minimal, buildable NeoForge 1.21.1 bootstrap at version `1.2.3`: Java 21, Gradle Wrapper 9.2.1, ModDevGradle 2.0.143, and NeoForge 21.1.240. Feature porting has not started yet; the legacy projects remain research inputs.

## Scope

The planned bundle is based on the behavior and content represented by these reference projects:

- Tinkers' Construct (`References/TinkersConstruct-1.20.1/`)
- Mantle (`References/Mantle-1.20/`)
- Construct's Armory (`References/constructsarmory-master/`)
- Moar Tinkers (`References/MoarTinkers-master/`)
- Oreberries (`References/Oreberries-1.12.1/`)
- TiC Tooltips (`References/TiC-Tooltips-1.7.10/`)
- Tinkers JEI (`References/TinkersJEI-master/`)
- Tinkers Tool Leveling (`References/TinkersToolLeveling-master/`)

The final scope, feature parity, registry IDs, integrations, and attribution requirements will be established as each feature is ported. Older APIs and data formats are references, not target implementations.

## Repository layout

```text
References/    Legacy mod source and assets used for research; read-only.
src/           New ModernTinkers NeoForge 1.21.1 code, resources, and tests.
build.gradle   NeoForge ModDevGradle build configuration.
gradle/        Checked-in Gradle Wrapper files.
gradle.properties  Target versions and mod metadata.
AGENTS.md      Detailed instructions for agents and contributors.
README.md      This project overview.
```

The source boundary is intentional: do not edit the projects under `References/`, even when their old build files or source do not work with current tooling. New code and port-specific adaptations belong in `src/`.

## Development

Use the checked-in Gradle Wrapper from the repository root:

```text
gradlew.bat build
gradlew.bat test
gradlew.bat runClient
```

The initial scaffold builds `ModernTinkers-1.21.1-1.2.3-NeoForge.jar` under `build/libs/`. The mod ID is `moderntinkers`, the Java package root is `com.moderntinkers`, and the metadata license is temporarily `UNLICENSED` until the project's licensing decision is made.

Every repository change must increment the build number in `gradle.properties` (for example, `1.2.3` to `1.2.4`) and produce the matching `ModernTinkers-1.21.1-<version>-NeoForge.jar` artifact.

A successful build is only one part of validation. A complete port also needs client and dedicated-server loading checks, fresh-world gameplay checks, content and recipe verification, optional-integration checks, and multiplayer checks where state is synchronized.

## Porting principles

- Target NeoForge and Minecraft 1.21.1 APIs directly.
- Keep the player-facing result as one coherent bundle.
- Trace reference behavior before adapting it; do not mechanically merge legacy projects.
- Prefer vanilla and NeoForge data systems for data-driven content.
- Keep client-only behavior out of dedicated-server code paths.
- Preserve source licenses, copyright notices, and required attribution for reused code and assets.
- Record meaningful intentional differences from the references.

See [AGENTS.md](AGENTS.md) for the full workflow, ownership rules, and validation checklist.
