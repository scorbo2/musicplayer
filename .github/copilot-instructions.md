# MusicPlayer - Copilot Coding Agent Instructions

## Repository Summary

**MusicPlayer** is a 100% Java music player with a Swing UI featuring customizable visualizations and extensive extension support. The application is built as a testbed for the [swing-extras](https://github.com/scorbo2/swing-extras) library's extension mechanism, demonstrating dynamic extension discovery, loading, and configuration.

**Key Features:**
- Customizable full-screen visualizations
- Dynamic extension loading via ExtensionManager
- Theme support via custom Look-and-Feel implementations
- Configuration-driven UI using PropertiesManager from swing-extras

**Repository Stats:**
- **Size:** ~2.9MB (excluding .git)
- **Language:** Java 17
- **Build Tool:** Maven 3.9+
- **Source Files:** 53 Java files (~8,337 lines of code)
- **Test Framework:** JUnit 5 (Jupiter)
- **Main Class:** `ca.corbett.musicplayer.Main`

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

### vavi-commons JAR Workaround

The `extra-jars/vavi-commons-1.1.10.jar` file is included to work around a broken jar in GitHub Packages. The pom.xml is configured to copy this jar to `target/lib` during the build (in the `validate` phase). This is intentional and documented in `extra-jars/readme.txt`.

## Build & Test Instructions

### Prerequisites
- **Java:** 17+ (JDK required for building)
- **Maven:** 3.9+ 
- **GitHub Authentication:** Required (see above)

### Build Commands

**Always run commands in this exact order:**

1. **Clean the project:**
   ```bash
   mvn clean
   ```

2. **Build the project (includes tests):**
   ```bash
   mvn package
   ```
   - **Duration:** ~30-60 seconds (first build downloads dependencies)
   - **Output:** `target/musicplayer-3.0.jar`
   - **Also creates:** `target/lib/` with all runtime dependencies

3. **Build without running tests:**
   ```bash
   mvn package -DskipTests
   ```

4. **Run tests only:**
   ```bash
   mvn test
   ```
   - **Test files:** Located in `src/test/java/ca/corbett/musicplayer/`
   - 6 test classes covering Actions, Extensions, UI components

### Running the Application

```bash
cd target
java -jar musicplayer-3.0.jar
```

**Note:** The application is a GUI application (Swing). It requires a display and cannot run in headless mode.

### Installer Generation (Optional)

If the [make-installer](https://github.com/scorbo2/install-scripts/) scripts are installed at `~/bin/make-installer` on Linux, the Maven build will automatically generate a tarball installer in `target/`.

Configuration is in `installer.props` at the repository root.

## Project Layout & Architecture

### Directory Structure

```
musicplayer/
├── .github/                     # GitHub configuration (this file)
├── docs/                        # User and developer documentation
│   ├── developer_overview.md   # Architecture and design patterns
│   ├── developer_extensions.md # Extension development guide
│   ├── developer_properties.md # Configuration system guide
│   ├── developer_exercise*.md  # Extension development tutorials
│   ├── user_guide_ui.md        # UI customization guide
│   └── user_guide_logging.md   # Logging configuration
├── extra-jars/                  # Workaround jars (vavi-commons)
├── src/
│   ├── main/
│   │   ├── java/ca/corbett/musicplayer/
│   │   │   ├── Main.java                    # Application entry point
│   │   │   ├── Version.java                 # Version constants
│   │   │   ├── AppConfig.java               # Configuration management (extends AppProperties)
│   │   │   ├── Actions.java                 # Action registration
│   │   │   ├── actions/                     # UI action implementations (18 classes)
│   │   │   ├── audio/                       # Audio playback & playlist logic
│   │   │   ├── extensions/                  # Extension system
│   │   │   │   ├── MusicPlayerExtension.java        # Base extension interface
│   │   │   │   ├── MusicPlayerExtensionManager.java # Extension loader
│   │   │   │   └── builtin/                 # Built-in extensions (themes, visualizers)
│   │   │   └── ui/                          # Swing UI components (19 classes)
│   │   │       ├── MainWindow.java          # Primary application window
│   │   │       ├── AudioPanel.java          # Waveform visualization
│   │   │       ├── ControlPanel.java        # Playback controls
│   │   │       ├── NowPlayingPanel.java     # Track info display
│   │   │       ├── Playlist.java            # Playlist component
│   │   │       └── VisualizationWindow.java # Full-screen visualizations
│   │   └── resources/ca/corbett/musicplayer/
│   │       ├── logging.properties           # Default logging configuration
│   │       ├── ReleaseNotes.txt            # Version history
│   │       └── images/                      # Application icons & logo
│   └── test/java/ca/corbett/musicplayer/   # JUnit 5 test classes
├── pom.xml                      # Maven project configuration
├── .editorconfig                # Code style configuration (IntelliJ format)
├── installer.props              # Installer generation config
├── update_sources.json          # Extension update sources
└── README.md                    # User-facing documentation
```

### Key Architectural Patterns

1. **Extension System:** Powered by `swing-extras` library's `ExtensionManager`
   - Extensions implement `MusicPlayerExtension` interface
   - Loaded from `EXTENSIONS_DIR` (default: `~/.MusicPlayer/extensions/`)
   - Can contribute configuration properties and UI components

2. **Configuration Management:** Uses `AppProperties` pattern from `swing-extras`
   - Properties defined in `AppConfig.java`
   - Automatically generates UI dialogs via `PropertiesManager`
   - Saved to `~/.MusicPlayer/MusicPlayer.props`

3. **Actions:** Swing Actions pattern for all user commands
   - Defined in `actions/` package (18 action classes)
   - Registered in `Actions.java`

4. **Audio Pipeline:** 
   - MP3 decoding via mp3spi library
   - Converted to WAV format internally (see TODO in `AudioData.java`)
   - Played via Java Sound API

### Configuration Files

- **pom.xml:** Maven configuration
  - Java 17 source/target
  - Key dependencies: swing-extras (2.5.0), mp3spi (1.9.14), JUnit Jupiter (5.12.1)
  - Includes javadoc generation with relaxed linting (`-Xdoclint:all -Xdoclint:-missing`)

- **.editorconfig:** Code style (4-space indentation, 120 char line length, LF line endings)

- **logging.properties:** Default log configuration (console + LogConsoleHandler)

- **installer.props:** Installer generation settings for make-installer script

## Validation & CI/CD

**No automated CI/CD workflows are currently configured.** There are no GitHub Actions workflows in `.github/workflows/`.

### Manual Validation Steps

1. **Build validation:** `mvn clean package` must succeed
2. **Test validation:** All tests in `src/test/` must pass
3. **Code style:** Follow .editorconfig conventions
4. **Javadoc validation:** Build generates javadoc without critical errors

### Code Quality Notes

- **TODOs exist:** See `AudioPanel.java`, `VisualizationThread.java`, `AudioData.java`, `AudioUtil.java`
- **Known issue:** MP3 to WAV conversion approach (see `AudioData.java` TODO)
- **Extension points:** Visualizers, themes, animations can be added via extensions

## Dependencies & External Libraries

### Runtime Dependencies (from pom.xml)
- **ca.corbett:swing-extras:2.5.0** - Core extension and UI framework
- **net.javazoom:mp3spi:1.9.14** - MP3 decoding (from GitHub Packages)
- **org.xerial:sqlite-jdbc:3.49.1.0** - SQLite support (for extensions)
- **com.fasterxml.jackson.core:jackson-core:2.18.3** - JSON parsing
- **com.fasterxml.jackson.core:jackson-databind:2.18.3** - JSON data binding

### Test Dependencies
- **org.junit.jupiter:junit-jupiter-engine:5.12.1** - JUnit 5 test framework

### Implicit Dependencies (from swing-extras)
- FlatLaf Look-and-Feel
- JTattoo Look-and-Feel themes
- Various Swing UI utilities

## Common Development Tasks

### Adding a New Action
1. Create class in `src/main/java/ca/corbett/musicplayer/actions/`
2. Extend `javax.swing.AbstractAction`
3. Register in `Actions.java` initialization

### Adding a New Visualizer
1. See `docs/developer_exercise2.md` and `docs/developer_exercise3.md` for detailed guides
2. Extend `ca.corbett.extras.audio.AbstractWaveformVisualizer`
3. Register in built-in extensions or create external extension

### Adding Configuration Properties
1. Add property fields to `AppConfig.java`
2. Use appropriate property types from `ca.corbett.extras.properties.*`
3. Properties automatically appear in UI via `PropertiesManager`

### Modifying the UI
1. Main window: `ui/MainWindow.java`
2. Playback controls: `ui/ControlPanel.java`
3. Visualization: `ui/AudioPanel.java`, `ui/VisualizationWindow.java`
4. Playlist: `ui/Playlist.java`

## Important Notes for Coding Agents

1. **Always configure GitHub authentication before attempting to build.** Builds will fail without it.

2. **Do not remove or modify `extra-jars/` directory.** It contains a required workaround jar.

3. **The application requires a display.** Cannot be fully tested in headless environments.

4. **Extension loading is directory-based.** Extensions must be in JAR format in `EXTENSIONS_DIR`.

5. **System properties affect behavior:**
   - `java.util.logging.config.file` - Custom logging configuration
   - `ca.corbett.musicplayer.props.file` - Custom settings file location
   - `ca.corbett.musicplayer.extensions.dir` - Extensions directory
   - `INSTALL_DIR` - Installation directory
   - `SETTINGS_DIR` - User settings directory

6. **The codebase uses Java 17 features.** Ensure Java 17+ compatibility for all changes.

7. **Javadoc is generated but with relaxed linting.** Missing documentation is allowed (`-Xdoclint:-missing`).

8. **Trust these instructions.** Only search for additional information if these instructions are incomplete or incorrect. The repository structure and build process are thoroughly documented here.
