## 2026-08-13

- Checked `Docs/TASK.md` before editing; it was empty.
- Updated `AGENTS.md` to require reading `Docs/TASK.md` before every task and appending a dated entry to `Docs/CHANGELOG.md`, `Docs/TRACELOG.md`, and `Docs/SUGGESTIONS.md` for every completed change.
- Validation: documentation-only change; `git diff --check` run after the edit.
- Updated `gradle.properties`, `build.gradle`, `README.md`, and the project logs for version `1.2.3` and the required `ModernTinkers-1.21.1-1.2.3-NeoForge.jar` output. The first clean build exposed Gradle's automatic version suffix (`...-NeoForge-1.2.3.jar`); `Jar.archiveFileName` now sets the exact requested filename.
- Validation passed: `gradlew.bat clean build --console=plain --no-daemon`; exactly one artifact was present in `build/libs/`, named `ModernTinkers-1.21.1-1.2.3-NeoForge.jar`; the archive contained `com/moderntinkers/ModernTinkers.class`, `META-INF/neoforge.mods.toml`, and `pack.mcmeta`; SHA-256 was `63fde0ceb9ba400f1cc0f3e409012646c3ce8c8237d2835d69fda32c92243cfd`.
