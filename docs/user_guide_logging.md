<-- [Back to musicplayer documentation](../README.md)

# User guide: configuring logging

If you started musicplayer using the command line, you probably noticed a few log messages getting generated
to the console:

```shell
$ java -jar musicplayer-2.8.jar 
2025-04-06 03:54:52 P.M. [INFO] Extension manager initialized with 4 active extensions.
2025-04-06 03:54:52 P.M. [INFO] Visualizer initialized on display 0
2025-04-06 03:54:52 P.M. [INFO] isFullscreenSupported: true
2025-04-06 03:54:59 P.M. [INFO] Starting visualization thread
2025-04-06 03:54:59 P.M. [INFO] VisualizationThread created; rendering at 1,920x1,080
```

If you started musicplayer by double-clicking the jar file, however, you probably didn't see any log output.
This is because, by default, log output goes to stdout only (and also to `LogConsole`, which we'll look at later).

The musicplayer application comes with a built-in `logging.properties`, which you can find in the resources
directory. You can, of course, override this by supplying your own. Let's look at a couple of ways to do this.

## Specifying your own logging.properties

Option 1 is to create a `logging.properties` file in the `.MusicPlayer` directory in your user home directory.
If this file is detected at startup, it will override the one that is built into the musicplayer jar file.

Option 2 is to explicitly find a home for `logging.properties` and specify it as a system property on startup.
For example:

```shell
$ java -Djava.util.logging.config.file=/path/to/wherever/logging.properties -jar musicplayer-2.8.jar
```

Great! Now I can see my log file get created every time I start up the application, so if something bad happens,
I can check it to see what went wrong.

## Using LogConsole

My `swing-extras` library (which musicplayer uses) includes a component called `LogConsole`. This provides a nice
way of viewing log information easily at runtime without having to go dig up the log file in whatever directory
you put it. The `LogConsole` also works even if you are not directing log output to a file, because it uses
its own log handler called `ca.corbett.extras.logging.LogConsoleHandler`.

To access the log console, hit ctrl+a to bring up the "About" dialog, or click on the toolbar button
with the MusicPlayer logo on it. This brings up the About dialog:

![About](screenshots/about.jpg "About")

The MusicPlayer logo image is clickable! Give it a click and you will see the `LogConsole` appear. You can 
summon it this way at any time, or just leave it up. It will update itself whenever new log information appears.

![LogConsole](screenshots/logconsole.jpg "LogConsole")
