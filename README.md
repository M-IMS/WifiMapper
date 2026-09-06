# WiFi Mapper (prototype)

A basic, fully offline WiFi signal-mapping app for your Galaxy Note 9. You load
a floor plan image, tap where you're standing, and it scans nearby WiFi
signal strength and marks a colored dot at that spot (green = strong,
red = weak). All data is stored only inside the app's private folder on your
phone — nothing is uploaded, and the app never requests internet access at all.

## What this version does
- Load any image as a floor plan (photo of a blueprint, or a rough sketch)
- Tap a spot on the map to log a WiFi reading there
- Colored dots show signal strength at each logged point
- Export a JSON copy of your data to the app's own external files folder
- Clear all points and start over

## What this version does NOT do (yet)
- No automatic positioning via camera/motion sensors — you tap your position
  manually as you walk around. Real indoor positioning (SLAM) is a much
  larger project; this gets you a working heatmap tool first.
- No smoothed/interpolated heatmap between points (just discrete dots) —
  can be added once the basic flow feels right.

## How to build and install it (step by step)

1. **Install Android Studio** (free): https://developer.android.com/studio
2. Open Android Studio → "Open" → select this `WifiMapper` folder.
3. Let Gradle sync (first time takes a few minutes, downloads dependencies).
4. Enable Developer Options on your Note 9: Settings → About phone → tap
   "Build number" 7 times.
5. In Developer Options, turn on **USB debugging**.
6. Plug the Note 9 into your computer via USB, allow the debugging prompt
   on the phone.
7. In Android Studio, select your Note 9 from the device dropdown (top
   toolbar) and click the green Run ▶ button.
8. The app installs and launches on your phone directly — no app store,
   no cloud, nothing leaves your machine except installing to your own device.

## First use
1. Tap "Load Floor Plan" and pick a photo/sketch of the area from your gallery.
2. Grant the location permission when asked — Android requires this to read
   WiFi scan results (it's an OS requirement for all apps, not something this
   app does with your location beyond that).
3. Walk to a spot, tap that spot on the on-screen map, wait ~1.5 seconds —
   a colored dot appears.
4. Repeat around the space to build up your heatmap.

## Where your data lives
`/data/data/com.example.wifimapper/files/readings.json` — private to this
app, inaccessible to other apps without root access. Exports go to the
app's own folder under Android/data on the phone's storage, viewable with
a file manager if you want to back them up yourself.

## Natural next steps (once this feels good)
- Smoothed heatmap overlay (interpolate between points) instead of dots
- Step-counter/gyroscope-assisted position tracking so you tap less often
- Per-network breakdown (see each router separately, not just the average)
