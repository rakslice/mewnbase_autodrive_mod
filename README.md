# mewnbase\_autodrive\_mod

This is an experimental auto-drive mod for MewnBase InDev 0.52.2.

It replaces the Buggie's special (handbrake) with an auto-drive feature that follows a path of striped pavement tiles.

<kbd>Space</kbd> - Turn on/off auto-drive

Auto-drive will turn off automatically when it runs out of striped pavement to follow.

## Building ##

Use the python script provided with your MewnBase game directory to build a replacement `desktop-1.0.jar` with the mod.

You'll need a JDK in your `JAVA_HOME`.

```
python build.py /path/to/itch/mewnbase
```

