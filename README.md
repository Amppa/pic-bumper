# Pic Bumper

**Pic Bumper** is an Android utility designed to solve the common issue where image pickers in apps like Bilibili, WeChat, and social media platforms only sort gallery images in reverse-chronological order (by newest timestamp). With Pic Bumper, users can pick any meme or image and bump its sequence to "Now", making it instantly appear at the very top of the system photo picker.

---

## Key Features

- **Bump to Top (Now)**: Writes images with current timestamps (`DATE_ADDED`, `DATE_MODIFIED`, `DATE_TAKEN`) and synchronizes EXIF `DateTimeOriginal` to guarantee top placement across diverse pickers.
- **Original Filename Preserved**: Retains the exact `DISPLAY_NAME` of original files without arbitrary renaming.
- **Dedicated Album (`Pictures/PicBumper`)**: All bumped images are organized under a dedicated album directory.
- **Silent Deletion for Self-Owned Assets**: When bumping images already located in `Pictures/PicBumper`, older versions are silently replaced without intrusive system dialogs.
- **External Image Safety**: Bumping images from external directories prompts whether to delete the original file, batching system authorization via `MediaStore.createDeleteRequest` into a single prompt.
- **Batch Interval Spacing**: Increments timestamps by 1-second intervals when multiple images are processed together to avoid database ordering collisions.
- **Native Android File Manager Integration**: Integrates directly with Android's system document browser (SAF), supporting folder navigation and native long-press multi-selection.
- **Memory Optimization**: Employs zero-decode binary byte streams and downsampled Coil thumbnail previews to maintain low memory usage and high frame rates.
- **Dark & AMOLED Night Mode**: Thoughtfully crafted Material 3 interface optimized for dark and pure black surfaces, gentle on the eyes during late-night commenting.
- **No Intrusive Toasts**: All status updates are displayed inline within the UI to maintain an uninterrupted workflow.

---

## Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM + Clean Architecture with Unidirectional Data Flow (UDF)
- **Image Loading**: Coil Compose with strict downsampling (`Scale.FIT`, `Precision.INEXACT`)
- **Settings Persistence**: Jetpack DataStore Preferences
- **System Compliance**: Android 10 to 15 Scoped Storage, `MediaStore` and `ExifInterface`

---

## Project Structure

```
pic-bumper/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   └── java/com/picbumper/
│   │       ├── MainActivity.kt
│   │       ├── PicBumperApplication.kt
│   │       ├── data/
│   │       │   ├── MediaBumperRepository.kt
│   │       │   └── SettingsRepository.kt
│   │       ├── domain/model/
│   │       │   ├── BumpSettings.kt
│   │       │   └── ImageItem.kt
│   │       └── ui/
│   │           ├── home/
│   │           │   ├── HomeScreen.kt
│   │           │   └── HomeViewModel.kt
│   │           ├── settings/
│   │           │   ├── SettingsScreen.kt
│   │           │   └── SettingsViewModel.kt
│   │           └── theme/
│   │               ├── Color.kt
│   │               ├── Theme.kt
│   │               └── Type.kt
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

---

## Getting Started

### Prerequisites
- Android Studio Iguana / Jellyfish or newer
- JDK 17
- Android SDK 34 (compileSdk: 34, minSdk: 24)

### Building the Project
Open the project root in Android Studio or run via command line:
```bash
./gradlew assembleDebug
```

---

## License

This project is licensed under the MIT License.
