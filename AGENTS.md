# MusicPlayer — Agent Instructions

## Build & Run

```
mvn package          # Requires Java 25+
java -jar target/musicplayer-4.1.jar   # Launch (after build)
```

Entry point: `ca.corbett.musicplayer.Main`

## Key quirks

- **vavi-commons jar**: The Maven Central/GitHub Packages version is broken (empty jar). `pom.xml` copies `extra-jars/vavi-commons-1.1.10.jar` into `target/lib/` during the build. Do not remove this step.
- **mp3spi dependency**: Resolved from GitHub Packages (`maven.pkg.github.com/umjammer/mp3spi`), not Maven Central.
- **swing-extras** (`ca.corbett:swing-extras:3.0.0`): External library providing `AppProperties`, `PropertiesManager`, `ExtensionManager`. The app's extensibility model is built on this.
- **Linux installer**: If `~/bin/make-installer` exists on Linux, the `make-installer` Maven profile auto-generates a tarball in `target/` after a successful build. Props in `installer.props`.
- **Dynamic extensions**: `update_sources.json` configures extension discovery URLs. Extensions are loaded from JARs at runtime via swing-extras `ExtensionManager`.

## Package structure

| Package | Purpose |
|---|---|
| `ca.corbett.musicplayer` | Core: `Main`, `AppConfig`, `Actions`, `Version` |
| `ca.corbett.musicplayer.audio` | Audio playback, metadata, playlists |
| `ca.corbett.musicplayer.actions` | Swing `AbstractAction` implementations |
| `ca.corbett.musicplayer.extensions` | Extension interfaces + built-in extensions (themes, visualizers, quick-load) |
| `ca.corbett.musicplayer.ui` | Swing UI: `MainWindow`, panels, visualizations, dialogs |

## Tests

```
mvn test
```

7 test classes under `src/test/java/ca/corbett/musicplayer/`. JUnit 5 (Jupiter). No special test fixtures or services required.

## Formatting

`.editorconfig` encodes full IntelliJ IDEA Java formatting rules (4-space indent, end-of-line braces, specific import order `@*,*,|,javax.**,java.**,|,$*`). No formatter plugin is configured in `pom.xml` — formatting is editor-driven.

## Docs

Developer guide lives in `docs/`:
- `developer_overview.md` — architecture overview (AppProperties, ExtensionManager)
- `developer_extensions.md`, `developer_properties.md` — extension/properties patterns
- `developer_exercise1.md` through `developer_exercise3.md` — walking tutorials for writing extensions and visualizers
