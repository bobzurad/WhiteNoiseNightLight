This is a simple app that allows one to use their phone as a night light. 

The app is currently published in the [Google Play](https://play.google.com/store/apps/details?id=net.zurad.bob.whitenoisenightlight) Store.

## Setup

To setup your build environment, install the Android SDK Command-Line Tools, Build Tools, and Platform Tools. 

For more information, see here: https://developer.android.com/tools

Also, add the following environment variables to your system:
```bash
export ANDROID_HOME=<path_to_android_sdk>
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
```

## Building Debug

To build an apk file that you can debug on your device (or emulator), run the following command from the Android directory:
```bash
  ./gradlew assembleDebug
```

This will put a file named `app-debug.apk` in `Android/app/build/outputs/apk/debug/`

## Building Release
To make a release build to install on your device (or emulator), run the following command from the Android directory:
```bash
  ./gradlew assemble
```

This will put a file named `app-release-unsigned.apk` in `Android/app/build/outputs/apk/release/`

## Signing a Release Build

Note: `zipalign` and `apksigner` will be in `$ANDROID_HOME/build-tools/<version>`
```
zipalign -v -p 4 app-release-unsigned.apk app-release-unsigned-aligned.apk
apksigner sign --ks <path_to_your_private_key>.jks --out app-release.apk app-release-unsigned-aligned.apk
apksigner verify app-release.apk
```

You can then upload `app-release.apk` to the Play Console when creating a Production Release.

For more information on building, deploying, and signing the apk file, see here: https://developer.android.com/build/building-cmdline
