# MusicPlayer - Copilot Coding Agent Instructions

## Repository Summary

**MusicPlayer** is a Java 17 music player with Swing UI featuring customizable visualizations and extension support via the [swing-extras](https://github.com/scorbo2/swing-extras) library. Built with Maven 3.9+, containing 53 Java files (~8,337 LOC), tested with JUnit 5. Main class: `ca.corbett.musicplayer.Main`.

## Critical Build Requirements & Known Issues

### GitHub Packages Authentication Requirement

**⚠️ CRITICAL:** This project has a dependency on `net.javazoom:mp3spi:1.9.14` from GitHub Packages that **requires authentication**. Maven builds will fail with a `401 Unauthorized` error if credentials are not configured.

**Solution:** Configure Maven authentication by creating `~/.m2/settings.xml`:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>github</id>
      <username>${env.GITHUB_ACTOR}</username>
      <password>${env.GITHUB_TOKEN}</password>
    </server>
  </servers>
</settings>
```

Set environment variables `GITHUB_ACTOR` (your GitHub username) and `GITHUB_TOKEN` (a GitHub Personal Access Token with `read:packages` permission).

**Alternative:** If you cannot authenticate, the build will fail at dependency resolution. This is a known limitation mentioned in `extra-jars/readme.txt` regarding the vavi-commons jar workaround.

**Note:** `extra-jars/vavi-commons-1.1.10.jar` is a workaround for a broken GitHub Packages jar (copied to `target/lib` during validate phase). Do not remove.

## Build & Test Instructions

**Prerequisites:** Java 17+, Maven 3.9+, GitHub authentication (required - see above)

**Commands (run in order):**
1. `mvn clean` - Clean project
2. `mvn package` - Build + tests (~30-60s first run) → `target/musicplayer-3.0.jar` + `target/lib/`
3. `mvn package -DskipTests` - Build without tests
4. `mvn test` - Run tests only (6 test classes in `src/test/java/ca/corbett/musicplayer/`)

**Run:** `cd target && java -jar musicplayer-3.0.jar` (requires display, not headless)

**Optional:** If `~/bin/make-installer` exists on Linux, build auto-generates installer tarball (config: `installer.props`).

## Project Layout & Architecture

**Key Directories:**
```
src/main/java/ca/corbett/musicplayer/
├── Main.java               # Entry point
├── AppConfig.java          # Config (extends AppProperties from swing-extras)
├── Actions.java            # Media player and playlist commands (unrelated to the actions/ package)
├── actions/                # 18 UI action classes
├── audio/                  # Audio playback, playlist (3 classes)
├── extensions/             # Extension system (MusicPlayerExtension, MusicPlayerExtensionManager, builtin/)
└── ui/                     # 19 Swing components (MainWindow, AudioPanel, ControlPanel, Playlist, etc.)
```

**Architecture:**
- **Extensions:** `swing-extras` ExtensionManager loads jars from `EXTENSIONS_DIR` (~/.MusicPlayer/extensions/)
- **Config:** AppProperties auto-generates UI from properties in `AppConfig.java` → saved to `~/.MusicPlayer/MusicPlayer.props`
- **Actions:** 18 Swing Actions in `actions/` - note that these are not related to `Actions.java` which is unrelated.
- **Audio:** MP3 decoded via mp3spi → WAV → Java Sound API (see TODO in `AudioData.java`)

**Config Files:** `pom.xml` (Maven), `.editorconfig` (4-space indent, 120 char lines), `logging.properties`, `installer.props`

## Validation & Dependencies

**No CI/CD configured.** Manual validation: `mvn clean package` succeeds, tests pass, follow .editorconfig style.

**Key Dependencies:** swing-extras (2.5.0), mp3spi (1.9.14), sqlite-jdbc (3.49.1.0), jackson (2.18.3), junit-jupiter (5.12.1)

**Code Quality:** TODOs in AudioPanel, VisualizationThread, AudioData, AudioUtil. MP3→WAV conversion approach needs improvement (see AudioData.java).

## Development Tasks

**New UI Action:** Create class in `actions/`, extend `AbstractAction`  
**New Visualizer:** See `docs/developer_exercise2.md`, extend `AbstractWaveformVisualizer`  
**New Config Property:** Add to `AppConfig.java` using `ca.corbett.extras.properties.*` types  
**Modify UI:** MainWindow, ControlPanel, AudioPanel, VisualizationWindow, Playlist in `ui/`
**New media player command:** Follow the pattern in `Actions.java`
**New playlist command:** Follow the pattern in `Actions.java`

## Documentation

- `README.md` - Overview, installation, links  
- `docs/developer_overview.md` - Architecture, swing-extras integration  
- `docs/developer_extensions.md` - Extension development  
- `docs/developer_exercise*.md` - Extension tutorials  
- `docs/user_guide_*.md` - UI customization, logging

## Critical Notes for Agents

1. **GitHub auth required for builds** - Configure ~/.m2/settings.xml with GITHUB_ACTOR and GITHUB_TOKEN
2. **Do not remove extra-jars/** - Contains required vavi-commons workaround
3. **GUI application** - Requires display, not headless-compatible
4. **Extension loading** - JARs from EXTENSIONS_DIR (~/.MusicPlayer/extensions/)
5. **System properties:** java.util.logging.config.file, INSTALL_DIR, SETTINGS_DIR, EXTENSIONS_DIR
6. **Java 17 required** - Ensure compatibility
7. **Javadoc linting relaxed** - Missing docs allowed (-Xdoclint:-missing)
8. **Trust these instructions** - Only search if incomplete/incorrect
