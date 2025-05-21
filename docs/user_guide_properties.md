<-- [Back to musicplayer documentation](../README.md)

# User guide: configuring properties location

By default, musicplayer will save its configuration to a file named `MusicPlayer.props` in the user's home directory.
If this file does not exist on startup, musicplayer will try to create it.

The location and name of this file can be changed, if you like. For example, suppose I want to have
a `.MusicPlayer` directory in my home directory, and I want the application to use a file named `app.config`
in that directory. All I need to do is specify that on startup:

```shell
$ java -Dca.corbett.musicplayer.props.file=/home/scorbett/.MusicPlayer/app.config -jar musicplayer-2.5.jar 
```

Now when I start the application, I see that file get created in that location, and when I make changes to the
application settings in the Properties dialog and save them, I see that the application will write its
config settings to that file. Great!

## Hand-editing the properties file

Not advisable, but in a pinch, you can hand-edit the application properties file, as it's just plain text.
Invalid values will generally be ignored in favor of default values, so you can't do much harm here.
But generally, the best way to update application configuration is through the Properties dialog.
