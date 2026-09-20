<p align="center">
  <img src="https://raw.githubusercontent.com/kainotch/Wammy/master/app/src/main/res/drawable-nodpi/wammy_logo.png" alt="Wammy Logo" width="200"/>
</p>

<h1 align="center">Wammy</h1>

<p align="center">
  <b>A free, open-source manga and novel reader for Android</b>
</p>

<p align="center">
  <a href="https://kainotch.github.io/wammy-otaku.github.io/"><b>Official Website & Documentation</b></a>
</p>

<p align="center">
  <a href="https://github.com/kainotch/Wammy/releases"><img src="https://img.shields.io/github/v/release/kainotch/Wammy?style=for-the-badge&logo=github&color=blue" alt="Latest Release"/></a>
  <a href="https://github.com/kainotch/Wammy/blob/main/LICENSE"><img src="https://img.shields.io/github/license/kainotch/Wammy?style=for-the-badge" alt="License"/></a>
</p>

---

## About

Wammy is a powerful, feature-rich reader application for Android that brings both manga and light novels together into a single, beautifully designed interface. Whether you are catching up on the latest manga chapters or diving into a new light novel, Wammy provides a seamless and customizable reading experience.

For full documentation, guides, and FAQ, please visit our [Official Website](https://wammy-otaku.github.io/).

## Features

### Reading
- **Manga & Novel Support:** Read both manga and light novels from a single app. Use the **Devil Fruit** button in the bottom right corner—just press, hold, and slide up to instantly switch between Manga and Novel sections!
  <br><br><img src="app/src/main/res/drawable/devil_fruit.png" width="80"/>
- **Multiple Source Support:** Browse and read from a wide variety of online sources.
- **Offline Reading:** Download chapters for reading without an internet connection.
- **Customizable Reader:** Adjust reading modes, orientation, background colors, and scale types to your preference.
- **Chapter Tracking:** Automatically track your reading progress and history.

### Discovery & Extensions
- **Global Search:** Search across all your installed sources simultaneously.
- **Extension Support:** Install extensions to add new manga and novel repositories.
- **JS Plugin System:** Extend app functionality with lightweight JavaScript plugins.
- **Clean UI:** Explore catalogs with an intuitive, Material You design.

### Library & Organization
- **Library Management:** Organize your collection with custom categories and filters.
- **Tracker Integration:** Automatically sync your reading progress with AniList, MyAnimeList, Kitsu, and more.
- **Migration Engine:** Easily migrate entries and progress between different sources.
- **Duplicate Detection:** Find and manage duplicate entries in your library.

### Personalization
- **Account Sync:** Sign in with your Google account for a personalized experience.
- **Custom Profile Pictures:** Set your own profile photo which syncs securely to the cloud.
- **Dynamic Theming:** Full dark mode support and Material You dynamic color theming that adapts to your device.
- **Customizable Navigation Button:** Fully resizable quick-switch button with a visual preview and custom haptic feedback tailored to your device.

### Advanced
- **Automatic Updates:** Stay up to date with the latest app features and extension fixes.
- **Backup & Restore:** Never lose your library, categories, or reading progress.
- **Download Manager:** Manage offline chapters with a dedicated download queue.

## Installation

1. Visit the [Wammy Download Page](https://wammy-otaku.github.io/download.html) or the [Releases](https://github.com/kainotch/Wammy/releases) tab.
2. Download the latest .apk file.
3. Enable "Install from Unknown Sources" in your Android settings if prompted.
4. Open the downloaded APK to install.
5. Launch Wammy and start reading.

## Building from Source

### Prerequisites
- Android Studio (latest stable release)
- JDK 17 or higher
- Android SDK with API level 35

### Build Steps

`ash
# Clone the repository
git clone https://github.com/kainotch/Wammy.git
cd Wammy

# Build the debug APK
./gradlew app:assembleDebug

# The APK will be generated at:
# app/build/outputs/apk/debug/app-debug.apk
`

## Credits

- **Developer:** [@kainotch](https://github.com/kainotch)
- **Partner:** [@xo._kiwikaffine](https://instagram.com/xo._kiwikaffine)

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.

## Links

- [Website & Docs](https://wammy-otaku.github.io/)
- [Report a Bug](https://github.com/kainotch/Wammy/issues)
- [Privacy Policy](https://wammy-otaku.github.io/privacy/)
