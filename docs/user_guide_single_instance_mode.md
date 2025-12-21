<-- [Back to musicplayer documentation](../README.md)

# Single instance mode

MusicPlayer can be run in "single instance mode", which ensures that only one instance of the 
application is running at any given time. If you try to start a second instance while one is 
already running, the new instance will simply send a message to the existing instance to bring 
its window to the front.

## Why would I want this?

The 3.1 release has better OS integration, allowing you to (depending on your operating system)
right-click on audio file(s) in your file manager and choose "Open with MusicPlayer" to open
them in MusicPlayer and start playing them immediately. However, most file explorers will
launch a new instance of the application each time you do this, which is not what you want.
With the new "single instance mode" feature, every time you select "Open with MusicPlayer",
the existing instance of MusicPlayer will be brought to the front, and the selected audio
file(s) will be added to the playlist and start playing.

## How do I enable single instance mode?

Single instance mode is enabled by default in the 3.1 release, so you don't have to do anything
special to enable it. If you want to disable it and allow multiple instances to run
simultaneously, you can uncheck the "Only allow a single instance of MusicPlayer" option
in the Preferences dialog, under the "General" tab, as shown here:

![Single instance mode](screenshots/single_instance_mode.png "Single instance mode")

