# MusicPlayer

This is "MusicPlayer", a 100% Java music player with a UI written in Java Swing. 

![MusicPlayer](docs/musicplayer.jpg "MusicPlayer")

Features:
- extremely customizable via Java extension classes
- highly configurable UI with application themes
- full-screen visualizations with many possibilities for extension
- very programmer-friendly if you want to write your own extensions

## The world doesn't need another music playing application!

Agreed. Go download VLC if you want something extremely solid and full-featured.

## So why does this project exist?

I wanted to play music on the tv in my living room, but with interesting and customizable full-screen visualization
so I could just leave it running and have something nice to look at while I do other stuff. With version 2.x, I 
also used musicplayer as a testbed for my own app-extensions library
(now part of [swing-extras](https://github.com/scorbo2/swing-extras))
so I could test out ways to make an application incredibly customizable via dynamically-loaded extensions. I also 
wanted to prove out the `AppProperties` class from `app-extensions` to really demonstrate how to develop an extremely 
customizable application while writing surprisingly little UI code (in the case of the properties dialog, and
the extension manager, almost literally none). With version 3.x, I'm also using this application as a testbed
for the new "dynamic extension discovery and download" feature from my swing-extras library. This allows
users to download, install, and update extensions right from within the UI of the application, without having
to clone, build, and install them manually.

## How do I get it?

You can download an installer tarball for Linux here:
- [MusicPlayer-3.0.tar.gz](http://www.corbett.ca/apps/MusicPlayer-3.0.tar.gz) TODO update link for 3.1
- Size: 21MB
- SHA-1: `6f65304e8a3160782fd8003df15b0f25da15af60` TODO update sha-1 for 3.1

Alternatively, you can clone the repo and build the project locally:

```shell
git clone https://github.com/scorbo2/musicplayer.git
cd musicplayer
mvn package
cd target
java -jar musicplayer-3.1.jar # Launch the application manually
```

If you have my [make-installer](https://github.com/scorbo2/install-scripts/) scripts installed and you are building
on Linux, the installer tarball will be generated for you automatically during a build and placed into `target`.
This is preferable to launching manually as shown above, at least when running on a Linux machine, because the
installer script will create a launcher wrapper script with proper environment variables set (and you get a nice
user-friendly desktop icon for easy launching).

## User guide

- [Customizing the UI](docs/user_guide_ui.md) 
- [Controlling log output](docs/user_guide_logging.md)
- **NEW in 3.0 release:** [Dynamically discovering and installing extensions](docs/user_guide_extension_manager.md)
- **NEW in 3.1 release:** [Single instance mode](docs/user_guide_single_instance_mode.md)

## Developer guide

- [General application design](docs/developer_overview.md)
- [Making use of `swing-extras` for application configuration](docs/developer_properties.md)
- [Making use of ExtensionManager to customize the app](docs/developer_extensions.md)
- [Exercise1: let's write a custom extension!](docs/developer_exercise1.md)
- [Exercise2: let's write a custom visualizer!](docs/developer_exercise2.md)
- [Exercise3: a non-trivial visualizer](docs/developer_exercise3.md)

## License

musicplayer is made available under the MIT license: https://opensource.org/license/mit

## Revision history

[Full release notes and version history](src/main/resources/ca/corbett/musicplayer/ReleaseNotes.txt)
