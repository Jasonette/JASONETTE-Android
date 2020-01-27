# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Sentimental Versioning](http://sentimentalversioning.org/).

## [3.0.0] Next Release

This version is the current in development. Will be released after the week of *6th November 2020*.

## [2.0.0](https://github.com/jasonelle/jasonelle/releases/tag/v2.0)

This version was released in November 2019.

### Added

- Hjson reading capability 

- Upgraded to AndroidX and better performance for Glide implementation

- onBackPressed avoiding closing the app on Back button press

### Changed

- Complete implementation for Calendar Demo

- Minimum Android version bumped to `6.0`.

- Changed from `QR_CODE` to `ALL_FORMATS` as suggested by `Mike Metcalfe`. [Docs](https://developers.google.com/android/reference/com/google/android/gms/vision/barcode/Barcode.html#ALL_FORMATS). With this addition now the android app can scan any barcode supported by the vision engine.

### Fixed

- Fixed issue when accessing Geolocation data. (Need to ask permissions).

- Fixed `build.gradle` for Android Studio `v3.2.1`. By [@CydeSwype](https://github.com/Jasonette/JASONETTE-Android/commits?author=CydeSwype).

- Fixed text zoom issue in `Webview` by [@naei](https://github.com/naei).

### Updated

### Removed

### Notes

This version is a complete overhaul focusing on 
modularization of the code and update of the libraries, improving the quality of the framework, maintaining the same json api.

### People

Huge thanks to the following persons that helped in this release:

- [Moises Portillo](https://github.com/moizest89): Helped with some guidance over the GPS permission issue.

- [Adán Miranda](https://github.com/takakeiji): Helped compiling *Jason* App APK.

- [Devs Chile](https://devschile.cl): Chilean commmunity of developers.

More people here [https://jasonelle.com/docs/#/folks](https://jasonelle.com/docs/#/folks).

## [1.0](https://github.com/jasonelle/jasonelle/releases/tag/v1.0)

First version of the *Jasonette* Mobile Framework.
