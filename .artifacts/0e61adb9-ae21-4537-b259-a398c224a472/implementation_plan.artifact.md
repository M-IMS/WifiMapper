# Implementation Plan - Migrate to AGP 9.0 and fix warnings

This plan migrates the project to use the modern AGP 9.0 features (Built-in Kotlin, New DSL) and resolves various IDE warnings in build files and source code.

## User Review Required

> [!IMPORTANT]
> - I am enabling "Built-in Kotlin" support introduced in AGP 9.0. This simplifies the build files by removing the need for the `kotlin-android` plugin.
> - I am updating `compileSdk` to 37 and `targetSdk` to 35 to stay current with Android standards.
> - I am updating several core dependencies to their latest stable versions.

## Proposed Changes

### Build Configuration

#### [MODIFY] [gradle.properties](file:///home/ims/Downloads/VSCODE/WifiMapper/gradle.properties)
- Enable built-in Kotlin by removing `android.builtInKotlin=false`.
- Enable the new DSL by removing `android.newDsl=false`.

#### [MODIFY] [settings.gradle](file:///home/ims/Downloads/VSCODE/WifiMapper/settings.gradle)
- Update `foojay-resolver-convention` to `1.0.0`.

#### [MODIFY] [build.gradle](file:///home/ims/Downloads/VSCODE/WifiMapper/build.gradle) (root)
- Remove `org.jetbrains.kotlin:kotlin-gradle-plugin` from classpath as it's now handled by AGP's built-in Kotlin support.

#### [MODIFY] [app/build.gradle](file:///home/ims/Downloads/VSCODE/WifiMapper/app/build.gradle)
- Remove `id 'org.jetbrains.kotlin.android'` from the `plugins` block.
- Update `compileSdk` to 37 and use the new `=` assignment syntax.
- Update `targetSdk` to 35.
- Migrate `kotlinOptions` to the new `kotlin.compilerOptions` block (or remove if defaults suffice).
- Update dependencies:
    - `androidx.core:core-ktx` to `1.19.0`
    - `androidx.appcompat:appcompat` to `1.8.0`
    - `com.google.android.material:material` to `1.14.0`
    - `androidx.constraintlayout:constraintlayout` to `2.2.2`

### Source Code Improvements

#### [MODIFY] [HeatmapView.kt](file:///home/ims/Downloads/VSCODE/WifiMapper/app/src/main/java/com/example/wifimapper/HeatmapView.kt)
- Implement `performClick()` to satisfy accessibility requirements when overriding `onTouchEvent`.
- Fix minor warnings (trailing commas, lifted assignment).

#### [MODIFY] [MainActivity.kt](file:///home/ims/Downloads/VSCODE/WifiMapper/app/src/main/java/com/example/wifimapper/MainActivity.kt)
- Remove unused import `androidx.core.app.ActivityCompat`.
- Fix string concatenation in `setText`.

#### [MODIFY] [WifiScanner.kt](file:///home/ims/Downloads/VSCODE/WifiMapper/app/src/main/java/com/example/wifimapper/WifiScanner.kt)
- Address `wifiManager.startScan()` deprecation (add comment about throttling or use suggested alternatives if available).
- Fix call chain performance (use `asSequence()`).

## Verification Plan

### Automated Tests
- Run `gradle sync` to ensure the migration is successful.
- Run `gradle assembleDebug` to verify the build.
- Run `analyze_file` on all modified files to ensure warnings are resolved.

### Manual Verification
- Deploy to a device/emulator to verify heatmap rendering and wifi scanning still function as expected.
