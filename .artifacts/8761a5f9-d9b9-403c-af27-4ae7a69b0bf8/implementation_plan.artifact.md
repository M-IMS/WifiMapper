# Implementation Plan - Modern & Minimal UI Redesign

Transform the WiFi Mapper UI into a modern, minimal experience using Material Design 3 principles. This includes decluttering the main screen and using immersive overlays.

## User Review Required

> [!IMPORTANT]
> **Minimalism**: I will move the toggle switches (Heatmap, Blank Map, Dark Mode) into a new "Settings" bottom sheet to keep the main mapping view clean.
>
> [!NOTE]
> **Primary Actions**: The bottom row of text buttons will be replaced by a clean action bar with icons and a primary Floating Action Button (FAB).

## Proposed Changes

### [Resources]

#### [NEW] Vector Icons
Create clean Material-style icons in `app/src/main/res/drawable/`:
- `ic_settings.xml`, `ic_delete.xml`, `ic_export.xml`, `ic_ar.xml`, `ic_map.xml`, `ic_close.xml`.

#### [MODIFY] [themes.xml](file:///home/ims/Downloads/VSCODE/WifiMapper/app/src/main/res/values/themes.xml) & [themes.xml (night)](file:///home/ims/Downloads/VSCODE/WifiMapper/app/src/main/res/values-night/themes.xml)
- Update to `Theme.Material3.DayNight.NoActionBar`.
- Define a clean color palette (primary Green, neutral backgrounds).

### [UI]

#### [MODIFY] [activity_main.xml](file:///home/ims/Downloads/VSCODE/WifiMapper/app/src/main/res/layout/activity_main.xml)
- **Top**: Add a subtle `MaterialToolbar` with the app name and a Settings icon.
- **Center**: The `HeatmapView` will now take up the full screen (behind the toolbar and FABs).
- **Bottom**:
    - A primary FAB for **AR Mode**.
    - Small circular icons for **Load Map**, **Clear**, and **Export**.
- Remove the cluttered `optionsBar` and `buttonBar`.

#### [NEW] [layout_settings_sheet.xml](file:///home/ims/Downloads/VSCODE/WifiMapper/app/src/main/res/layout/layout_settings_sheet.xml)
- A Modal Bottom Sheet layout containing the "Visual Heatmap", "Blank Map", and "Dark Mode" switches.

#### [MODIFY] [activity_ar.xml](file:///home/ims/Downloads/VSCODE/WifiMapper/app/src/main/res/layout/activity_ar.xml)
- Make the `arStatusText` smaller and use a rounded chip-like background.
- Use a large circular "Shutter" style button for **Log Point**.
- Replace the "Exit AR" button with a simple close icon at the top corner.

### [Logic]

#### [MODIFY] [MainActivity.kt](file:///home/ims/Downloads/VSCODE/WifiMapper/app/src/main/java/com/example/wifimapper/MainActivity.kt)
- Implement `BottomSheetDialog` to handle settings.
- Update view bindings to the new UI components.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to verify compilation.

### Manual Verification
1.  **Immersive Map**: Verify the map view feels larger and less cluttered.
2.  **Settings Sheet**: Open the settings and verify all toggles (Heatmap, Dark Mode, etc.) still work.
3.  **AR Experience**: Verify the AR UI feels more like a modern camera app.
4.  **Responsiveness**: Ensure the icons and FABs are well-positioned on the screen.
