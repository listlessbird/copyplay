# Copyplay

Copyplay is an Android video player for [Copyparty](https://github.com/9001/copyparty). Connect it to your server, browse your files, and play videos without downloading them first. It supports subtitles, folder playback, and resuming where you left off.

## Build

You need JDK 17 and the Android SDK. Clone the repository, then run:

```sh
./gradlew testDebugUnitTest assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.
