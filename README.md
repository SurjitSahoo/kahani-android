# <img src="metadata/brandassets/app.icon.v2.with.shadow.png" width="40" height="40" align="center"> Kahani - Audiobook Player for Audiobookshelf

[![Build App](https://github.com/SurjitSahoo/kahani-android/actions/workflows/build_app.yml/badge.svg)](https://github.com/SurjitSahoo/kahani-android/actions/workflows/build_app.yml)

**Kahani** (meaning "story" in Hindi) is an offline-first Android audiobook player for [Audiobookshelf](https://github.com/advplyr/audiobookshelf) servers.

## ✨ Features

|     | Feature                | Description                                                                    |
| --- | ---------------------- | ------------------------------------------------------------------------------ |
| 🎨  | **Material Design 3**  | Beautiful, modern UI with dynamic color theming                                |
| 📡  | **Stream & Download**  | Stream from server or download for offline playback                            |
| 🔄  | **Auto Sync**          | Listening progress syncs automatically across all devices                      |
| ⏩  | **Playback Controls**  | Variable speed, skip silence, customizable seek intervals                      |
| 📖  | **Chapter Navigation** | Browse and jump between chapters with progress tracking                        |
| 🌐  | **Network-Aware**      | Automatically detects network changes and server status and syncs the progress |
| 🔲  | **Home Widget**        | Control playback directly from your home screen                                |
| 😴  | **Sleep Timer**        | Fall asleep listening with timer or end-of-chapter stop                        |
| 🎙️  | **Podcast Support**    | Full support for both audiobooks and podcasts                                  |
| 🌗  | **Dark/Light Theme**   | Follows your system theme preference automatically                             |

## 📱 Screenshots

<div align="center">
  <table>
    <tr>
      <td align="center"><img src="metadata/en-US/images/phoneScreenshots/library.png" width="200" style="display: block; margin: 0 auto;"><div style="margin-top: -10px">Library</div></td>
      <td align="center"><img src="metadata/en-US/images/phoneScreenshots/book.details.with.player.png" width="200" style="display: block; margin: 0 auto;"><div style="margin-top: -10px">Book Details</div></td>
      <td align="center"><img src="metadata/en-US/images/phoneScreenshots/book.details.with.payer.2.png" width="200" style="display: block; margin: 0 auto;"><div style="margin-top: -10px">Chapter List</div></td>
    </tr>
    <tr>
      <td align="center"><img src="metadata/en-US/images/phoneScreenshots/player.1.png" width="200" style="display: block; margin: 0 auto;"><div style="margin-top: -10px">Player</div></td>
      <td align="center"><img src="metadata/en-US/images/phoneScreenshots/player.2.png" width="200" style="display: block; margin: 0 auto;"><div style="margin-top: -10px">Customizable Controls</div></td>
      <td align="center"><img src="metadata/en-US/images/phoneScreenshots/settings.png" width="200" style="display: block; margin: 0 auto;"><div style="margin-top: -10px">Settings</div></td>
    </tr>
  </table>
</div>

## 🚀 Building

### 1. Clone the repository

```bash
git clone https://github.com/SurjitSahoo/kahani-android.git
cd kahani-android
```

### 2. Set up `local.properties`

Create a `local.properties` file in the project root with the following:

```properties
# Required: Android SDK path
sdk.dir=/path/to/your/Android/sdk

# Optional: Release signing configuration (for signed builds)
RELEASE_STORE_FILE=/path/to/your/keystore.jks
RELEASE_STORE_PASSWORD=your_keystore_password
RELEASE_KEY_ALIAS=your_key_alias
RELEASE_KEY_PASSWORD=your_key_password
```

> **Note:** The release signing config is only needed if you want to create signed release builds. Debug builds work without it.

### 3. Build the app

```bash
./gradlew assembleDebug    # Debug build
./gradlew assembleRelease  # Release build (requires signing config)
```

### 4. Install on device

```bash
./gradlew installDebug    # Install debug build
./gradlew installRelease  # Install release build (requires signing config)
```

## 🤝 Contributing

Contributions are welcome! Feel free to open issues or submit pull requests.

## 💝 Support

If you find Kahani useful, consider supporting the project:

### Ko-fi

<p align="center">
  <a href="https://ko-fi.com/surjitsahoo">
    <img height='50' src='https://storage.ko-fi.com/cdn/kofi6.png?v=6' alt='Buy Me a Coffee at ko-fi.com' />
  </a>
</p>

### UPI (India)

<p align="center">
  <img src="metadata/brandassets/upi/UPI_S.png" alt="UPI" height="40"><br><br>
  <img src="metadata/UPI.QR.white.png" alt="UPI QR Code" width="200">
</p>

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
