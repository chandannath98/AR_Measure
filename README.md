# 📏 AR Measure — Kotlin Jetpack Compose

A production-grade **augmented reality measuring app** for Android, built with Kotlin, Jetpack Compose, and ARCore. Turn your phone into a digital measuring tape.

---

## ✨ Features

| Feature | Detail |
|---|---|
| **AR Length Measurement** | Tap two points in AR space, get the real-world distance |
| **Height Measurement** | Measure vertical distances — ideal for person height |
| **Spirit Level** | Accelerometer-based bubble level with pitch + roll readout |
| **Unit Toggle** | Switch between cm, inches, meters, feet with one tap |
| **Measurement History** | Last 10 results saved in a slide-up drawer |
| **Plane detection** | Horizontal + vertical surfaces highlighted |
| **Feature point cloud** | Visual scanning feedback |

---

## 🏗️ Architecture

```
ARMeasureApp/
├── ar/
│   ├── ArSessionManager.kt        # ARCore Session lifecycle + hit-testing
│   ├── ArRenderer.kt              # GLSurfaceView.Renderer (camera + anchors)
│   ├── BackgroundRenderer.kt      # Camera feed as OpenGL texture
│   ├── AnchorDotRenderer.kt       # Dots + line between measurement points
│   ├── PlaneAndPointRenderers.kt  # Plane highlight + feature-point cloud
│   ├── DisplayRotationHelper.kt   # Handles display rotation for ARCore
│   └── LevelSensorHelper.kt       # Accelerometer → pitch / roll flow
│
├── model/
│   └── Models.kt                  # Data classes, enums, extension fns
│
├── viewmodel/
│   └── MeasureViewModel.kt        # UI state, unit formatting, history
│
└── ui/
    ├── theme/Theme.kt             # Dark high-tech color scheme + typography
    ├── components/
    │   ├── UiComponents.kt        # MeasurementBubble, Reticle, ModeChip…
    │   └── ArSurfaceViewLifecycle.kt  # Lifecycle-aware GLSurfaceView in Compose
    └── screens/
        ├── AppNavHost.kt          # Splash → Measure navigation
        ├── SplashScreen.kt        # Animated intro screen
        ├── MainMeasureScreen.kt   # Main AR measurement screen
        ├── LevelScreen.kt         # Spirit level overlay
        ├── PermissionScreen.kt    # Camera permission gate
        └── HistoryDrawer.kt       # Slide-up measurement history
```

---

## 🚀 Setup

### Prerequisites

| Requirement | Version |
|---|---|
| Android Studio | Ladybug (2024.2) or newer |
| Android SDK | API 26+ (minSdk) |
| Android SDK Target | API 35 |
| Kotlin | 2.1.0 |
| Physical Device | ARCore-supported Android device |

> ⚠️ **ARCore does NOT work on emulators.** You need a physical device.

### Steps

```bash
# 1. Clone / open the project in Android Studio
# 2. Let Gradle sync (it downloads all dependencies automatically)
# 3. Enable USB Debugging on your phone
# 4. Connect device and press Run ▶
```

### ARCore Device Requirement

This app uses `<uses-feature android:name="android.hardware.camera.ar" android:required="true" />`, which means the Play Store will only offer it to ARCore-capable devices.

To test on any ARCore device:
1. Make sure **Google Play Services for AR** (ARCore) is installed on the device.
2. If prompted by ARCore to install or update — accept it.

---

## 🎨 Design System

The UI uses a **dark high-tech aesthetic**:

| Token | Value |
|---|---|
| Background | `#060A12` near-black blue |
| Accent (cyan) | `#00D4FF` |
| Accent 2 (orange) | `#FF6B35` |
| Measurement yellow | `#FFE53B` |
| Success green | `#3DFF9A` |

---

## 📐 How Measurement Works

1. **ARCore session** initialises with `HORIZONTAL_AND_VERTICAL` plane finding.
2. On each frame, `ArSessionManager.update()` calls `session.update()` and reads `TrackingState`.
3. When the user taps, `frame.hitTest(x, y)` finds the real-world 3D intersection.
4. A **first anchor** is placed at the hit pose.
5. The user taps again; a **second anchor** is placed.
6. Distance = `sqrt(Δx² + Δy² + Δz²)` between the two anchor poses.
7. The result is displayed in a glowing bubble overlay.

---

## 🧪 Troubleshooting

| Issue | Solution |
|---|---|
| "AR not supported" | Device must support ARCore. Check [ARCore supported devices](https://developers.google.com/ar/devices) |
| Black screen | Make sure camera permission is granted |
| Session won't start | Install/update Google Play Services for AR |
| Inaccurate measurements | Move slower during scanning; ensure good lighting |
| Build fails on GLSurfaceView | Confirm `compileSdk = 35` and all dependencies synced |

---

## 📦 Key Dependencies

```
com.google.ar:core:1.46.0               # ARCore
com.google.accompanist:accompanist-permissions  # Permission helpers
androidx.compose:compose-bom:2024.12.01  # Jetpack Compose BOM
androidx.lifecycle:lifecycle-viewmodel-compose  # ViewModel + Compose
```

---

## 📝 License

MIT — free to use and modify.
# AR_Measure
