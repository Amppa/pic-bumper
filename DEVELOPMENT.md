# Developer & AI Agent Technical Documentation

This document contains in-depth architecture details, system compliance strategies, implementation decisions, and guidelines for software engineers and AI assistants maintaining or extending **Pic Bumper**.

---

## 1. System Architecture & Core Mechanics

Pic Bumper uses a **Clean Architecture + MVI/MVVM** approach with Unidirectional Data Flow (UDF).

```
                      ┌─────────────────────────┐
                      │    UI Layer (Compose)   │
                      │  - MainActivity         │
                      │  - HomeScreen           │
                      │  - SettingsScreen       │
                      └────────────┬────────────┘
                                   │ Observes UiState via StateFlow
                                   ▼
                      ┌─────────────────────────┐
                      │     ViewModel Layer     │
                      │  - HomeViewModel        │
                      │  - SettingsViewModel    │
                      └────────────┬────────────┘
                                   │ Calls Repositories
                                   ▼
                      ┌─────────────────────────┐
                      │       Data Layer        │
                      │  - MediaBumperRepository│ ◄── ContentResolver / MediaStore
                      │  - SettingsRepository   │ ◄── Jetpack DataStore Preferences
                      └─────────────────────────┘
```

---

## 2. Technical Decisions & MediaStore Strategy

### A. Timestamp Triad Override (`DATE_ADDED`, `DATE_MODIFIED`, `DATE_TAKEN`)
Different OEM galleries and third-party image pickers (such as Bilibili, WeChat, Matisse, PictureSelector) sort media queries differently:
- Some sort by `MediaStore.Images.Media.DATE_MODIFIED DESC`.
- Others sort by `MediaStore.Images.Media.DATE_ADDED DESC`.
- System galleries often sort by `MediaStore.Images.Media.DATE_TAKEN DESC` (milliseconds).

**Implementation**:
`MediaBumperRepository.kt` writes all three fields simultaneously to the current system epoch:
```kotlin
values.put(MediaStore.Images.Media.DATE_ADDED, timestampSec)
values.put(MediaStore.Images.Media.DATE_MODIFIED, timestampSec)
values.put(MediaStore.Images.Media.DATE_TAKEN, timestampMillis)
```
If the image is a JPEG and EXIF is enabled in settings, `ExifInterface` additionally rewrites `TAG_DATETIME`, `TAG_DATETIME_ORIGINAL`, and `TAG_DATETIME_DIGITIZED`.

### B. Batch Timestamp Offset Algorithm
When multiple memes are bumped in one batch, inserting identical timestamps can cause database ordering ambiguity.
To ensure the items strictly reflect the user's selected sequence:
$$\text{Timestamp}_i = \text{BaseNow} + (i \times 1\text{s})$$

### C. Zero-Decode Binary Stream Transfer
To avoid out-of-memory errors (OOM) and quality loss, and to retain full GIF/WebP animation frames and transparency:
- Images are transferred strictly via raw binary streams without `BitmapFactory` decoding:
  ```kotlin
  contentResolver.openInputStream(sourceUri)?.use { input ->
      contentResolver.openOutputStream(targetUri)?.use { output ->
          input.copyTo(output, bufferSize = 8192)
      }
  }
  ```

### D. Scoped Storage & Directory A (`Pictures/PicBumper`) Lifecycle
- **Directory A (`Pictures/PicBumper`)**: All bumped images are placed into this public album.
- **Media Permissions (`READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE`)**:
  - Required to read existing images in `Pictures/PicBumper` created by previous app installations (or across re-installs).
  - Automatically requested on startup or manual refresh.
- **Self-Owned Asset Privilege**: Files created within `Pictures/PicBumper` by the current installation are owned by Pic Bumper under Android Scoped Storage CDD guidelines.
  - When a self-owned asset already in Directory A is re-bumped, `contentResolver.delete(oldUri, null, null)` executes **silently without any system dialog**.
- **External Asset Safety & Reinstall Fallback**:
  - Files originating from other directories (e.g., `Downloads`, `DCIM`) or from previous installations where ownership was severed require user consent.
  - Deletions are batched through `MediaStore.createDeleteRequest` (Android 11+ / API 30+) to prompt only once for all external or previous items.

### E. Memory & GPU Pipeline Optimization (Coil 2.x & Downsampling)
Thumbnails in `PicBumperApplication.kt` and `ImageGridCard.kt` strictly configure hardware acceleration:
- **Global ImageLoader**: Configures 20% RAM memory cache, 250MB disk cache, and `.allowHardware(true)` for zero-copy GPU rendering pipeline.
- **3-Tier Configurable Downsampling**: 100px (ultra-fast), 150px (balanced - default), and 240px (high-res).
- **Cache Key Invalidation**: `memoryCacheKey("${uri}_${thumbnailSize}")` ensures exact cache resolution matching when switching quality settings.
- **Zero-RAM Dimension Fallback**: `BitmapFactory.Options(inJustDecodeBounds = true)` parses width/height without allocating bitmap memory.

### F. Duplicate Import Collision Resolution
When importing external files with matching filenames in `Pictures/PicBumper`:
- `DuplicateImportDialog` presents side-by-side thumbnail comparison with file size and modified timestamps.
- **Keep Both**: Auto-names new file to `name (1).ext`.
- **Replace**: Deletes old file and overwrites with incoming file.
- **Skip**: Cancels import for duplicate file.

---

## 3. Project Structure

```
pic-bumper/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   └── java/com/picbumper/
│   │       ├── MainActivity.kt               # Entry point, navigation, and IntentSender handling
│   │       ├── PicBumperApplication.kt        # Application class & Coil ImageLoaderFactory
│   │       ├── data/
│   │       │   ├── MediaBumperRepository.kt   # MediaStore insertion, stream copy, EXIF, deletion
│   │       │   └── SettingsRepository.kt      # DataStore Preferences persistence
│   │       ├── domain/model/
│   │       │   ├── BumpSettings.kt            # Settings data model and thumbnail size preference
│   │       │   └── ImageItem.kt               # @Immutable media item model with metadata
│   │       ├── ui/
│   │       │   ├── home/
│   │       │   │   ├── HomeScreen.kt          # Main gallery grid and dialog orchestration
│   │       │   │   ├── HomeViewModel.kt       # UiState and collision/bump orchestration
│   │       │   │   └── components/
│   │       │   │       ├── DuplicateImportDialog.kt # Side-by-side import collision dialog
│   │       │   │       ├── EmptyAlbumView.kt   # Empty state view
│   │       │   │       ├── FloatingStatusCapsule.kt # Floating status pill overlay
│   │       │   │       ├── HomeDialogs.kt      # Rename and delete confirm dialogs
│   │       │   │       ├── HomeTopBar.kt       # Multi-select & main action top bar
│   │       │   │       ├── ImageGridCard.kt    # LazyGrid card with hardware bitmaps
│   │       │   │       └── ImagePreviewDialog.kt # Full-screen preview with top-left Info card
│   │       │   ├── settings/
│   │       │   │   ├── SettingsScreen.kt      # Material 3 preference screen & 3-tier thumbnail dialog
│   │       │   │   └── SettingsViewModel.kt   # Preference mutation bindings
│   │       │   └── theme/
│   │       │       ├── Color.kt               # AMOLED pure black palette
│   │       │       ├── Theme.kt               # System bar & theme styling
│   │       │       └── Type.kt                # Typography tokens
│   │       └── util/
│   │           └── FormatUtils.kt             # Shared file size and date formatting utilities
│   └── build.gradle.kts                       # App module configuration
├── .github/workflows/
│   └── build-apk.yml                          # GitHub Actions cloud APK compiler
├── gradle/
│   ├── libs.versions.toml                     # Version catalog
│   └── wrapper/
│       └── gradle-wrapper.properties
├── build.gradle.kts                           # Root Gradle configuration
├── settings.gradle.kts                        # Plugin management & project include
├── DEVELOPMENT.md                             # This developer & AI guide
└── README.md                                  # End-user documentation
```

---

## 4. Building and Testing

### Build via Command Line
```bash
./gradlew assembleDebug
```
Output location:
`app/build/outputs/apk/debug/app-debug.apk`

### Cloud Build (GitHub Actions)
Triggered automatically on pushes to `master`/`main` or manually via the GitHub Actions tab.
Artifacts are stored under `PicBumper-Debug-APK`.
