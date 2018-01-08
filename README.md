Video Player plugin for Cordova/PhoneGap
========================================

A Codova plugin that simply allows you to immediately play a video in fullscreen mode.


# Installation

This plugin use the Cordova CLI's plugin command. To install it to your application, simply execute the following (and replace variables).

```
cordova plugin add cordova-plugin-video-player
```


# Using

Just call the  `play` method with a video file path as argument. The video player will close itself when the video will be completed.

```
VideoPlayer.play(path, [options], [completeCallback], [errorCallback]);
```

Stop and close a video currently playing without waiting the end.
```
VideoPlayer.close();
```

The plugin is able to play file-path or http/rtsp URL.

You can optionally add options parameters like volume and calling mode.
You can also add an success callback function to handle completed playback.
You can also add an error callback function to handle unexpected playback errors.

## Example

```javascript
VideoPlayer.play("file:///android_asset/www/movie.mp4");
```

```javascript
VideoPlayer.play(
    "file:///android_asset/www/movie.mp4",
    {
        volume: 0.5,
        scalingMode: VideoPlayer.SCALING_MODE.SCALE_TO_FIT_WITH_CROPPING
    },
    function () {
        console.log("video completed");
    },
    function (err) {
        console.log(err);
    }
);
```

## Options

- `volume`: (Optional) allows you to set the volume on this player. Note that the passed volume value is raw scalars in range 0.0 to 1.0.

- `scalingMode`: (Optional) allows you to sets video scaling mode.

    The following constants are the only values available for the `scalingMode` option:

    - `SCALE_TO_FIT` (default)
    - `SCALE_TO_FIT_WITH_CROPPING`

    Refer to http://developer.android.com/reference/android/media/MediaPlayer.html#setVideoScalingMode(int) for more details.

