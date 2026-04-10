# SolGuard: Technical Approach Document

## 1. Overview

**SolGuard** is a fully offline Android app that estimates real-time UV index using phone sensors and provides sun safety guidance: safe exposure time based on skin type, Vitamin D synthesis time, and WHO-standard recommendations.

**Key design constraints:**
- Zero internet dependency — works in airplane mode
- No ML models — pure algorithmic computation
- Tiny footprint — ~5 MB APK (Compose runtime is the bulk)
- All knowledge embedded as JSON assets (~5 KB total)

```
┌─────────────────────────────────────────────────┐
│                  ANDROID APP                     │
│                                                  │
│  Sensors          Algorithm           UI         │
│  ┌──────────┐    ┌──────────────┐   ┌─────────┐ │
│  │ GPS      │───►│ NOAA Sun     │──►│ Home    │ │
│  │ Light    │───►│ Position     │   │ Screen  │ │
│  │ Barometer│───►│ Calculator   │   │ (UV     │ │
│  │ Compass  │    │      │       │   │  gauge, │ │
│  └──────────┘    │      ▼       │   │  advice)│ │
│                  │ UV Index     │   ├─────────┤ │
│  Knowledge       │ Calculator   │   │ Details │ │
│  ┌──────────┐    │ (Beer-Lambert│──►│ Screen  │ │
│  │skin_types│───►│  + ozone +   │   ├─────────┤ │
│  │uv_safety │    │  altitude +  │   │Settings │ │
│  │ozone_fac.│    │  cloud corr.)│   │ Screen  │ │
│  └──────────┘    └──────────────┘   └─────────┘ │
└─────────────────────────────────────────────────┘
```

---

## 2. Algorithm: UV Index Estimation

### 2.1 Step 1 — Solar Zenith Angle (from NOAA)

The NOAA solar position algorithm computes the sun's altitude and azimuth from:
- **Latitude & longitude** (from GPS)
- **UTC date & time** (from system clock)

Solar Zenith Angle (SZA) = 90° - sun altitude. This is the primary input for UV estimation because it determines the atmospheric path length UV rays travel through.

The implementation is a direct Kotlin port of the NOAA Solar Calculator spreadsheet (~110 lines of trigonometry). It was originally written for the [Sunleaf](https://github.com/user/sunleaf) project and is reused here unchanged.

### 2.2 Step 2 — Clear-Sky Base UV

The empirical relationship between SZA and UV index under clear skies follows from the Beer-Lambert law of atmospheric absorption:

```
Base UV ≈ 12.0 × cos(SZA)^2.4
```

At solar noon (SZA ≈ 0°-30°), base UV reaches 10-12. As the sun drops below 60° altitude, UV falls quickly.

### 2.3 Step 3 — Correction Factors

| Factor | Formula | Data Source |
|--------|---------|-------------|
| **Altitude** | `1.0 + 0.06 × (altitude_m / 1000)` | Barometer sensor via `SensorManager.getAltitude()` |
| **Cloud cover** | `clamp(ambientLux / expectedClearSkyLux, 0.25, 1.0)` | Phone light sensor (`TYPE_LIGHT`) |
| **Ozone** | Seasonal lookup table (6 latitude bands × 12 months) | Embedded `ozone_factors.json` |
| **Surface** | Multiplier: grass=1.0, sand=1.15, water=1.10, snow=1.25 | User setting |

### 2.4 Step 4 — Final UV Index

```
UV Index = Base UV × altitude_correction × cloud_factor × ozone_factor × surface_factor
```

Clamped to [0, 15] and rounded to one decimal place.

### 2.5 Safe Exposure Time

Based on the Fitzpatrick skin type scale (Types I-VI) and the standard Minimal Erythemal Dose (MED):

```
Safe minutes (no sunscreen) = (MED[skin_type] × 40) / UV_index
Safe minutes (with SPF)     = safe_minutes × min(SPF, 50)
```

| Skin Type | MED (SED) | Example at UV 8 |
|-----------|-----------|-----------------|
| I — Very fair | 2.0 | ~10 min |
| II — Fair | 2.5 | ~12 min |
| III — Medium | 3.5 | ~17 min |
| IV — Olive | 4.5 | ~22 min |
| V — Brown | 6.0 | ~30 min |
| VI — Dark | 10.0 | ~50 min |

### 2.6 Vitamin D Timer

```
Minutes for 1000 IU = 1000 / (base_rate[skin_type] × UV_index / 5)
```

Where `base_rate` is the IU/minute production rate for face+arms exposure at UV 5.

### 2.7 Daily UV Curve

The app computes UV index for each hour (5am-8pm) using the NOAA calculator at `cloudFactor = 1.0` (clear sky forecast), giving users a profile of today's UV showing peak time and safe windows.

---

## 3. Sensor Inputs

| Sensor | Android API | What It Provides |
|--------|-------------|-----------------|
| GPS | `FusedLocationProviderClient` | Latitude, longitude for solar geometry |
| Light | `Sensor.TYPE_LIGHT` | Ambient lux — cloud cover proxy |
| Barometer | `Sensor.TYPE_PRESSURE` | Pressure in hPa → altitude via barometric formula |
| Compass | `Sensor.TYPE_ROTATION_VECTOR` | Device heading (used for sun direction context) |

All sensors degrade gracefully — if a sensor is unavailable, defaults are used (sea level altitude, clear sky assumption, etc.).

---

## 4. Coding Approach

### 4.1 Reuse from Sunleaf

SolGuard reuses the following from the Sunleaf plant care app:

| Component | Source File | Reuse Level |
|-----------|------------|-------------|
| `SunPositionCalculator.kt` | Sunleaf `sunlight/` | Package rename only |
| `SensorReader.kt` | Sunleaf `BrightnessEstimator.kt` | Added barometer sensor |
| Build configuration | Sunleaf `build.gradle.kts` | Dropped CameraX + TFLite |
| Navigation pattern | Sunleaf `Navigation.kt` | 3 screens instead of 6 |
| Permission handling | Sunleaf `MainActivity.kt` | Dropped CAMERA permission |

### 4.2 New Code

| File | Lines | Purpose |
|------|-------|---------|
| `UVIndexCalculator.kt` | ~160 | Core UV math: estimation, exposure, Vitamin D, daily curve |
| `UVDataRepository.kt` | ~90 | JSON asset reader for skin types and safety data |
| `HomeScreen.kt` | ~300 | Main UI: UV gauge, recommendations, exposure card, UV curve |
| `DetailsScreen.kt` | ~130 | Technical breakdown of all calculation factors |
| `SettingsScreen.kt` | ~160 | Skin type, SPF, surface type picker |

### 4.3 Architecture

```
MainActivity
    └── SolGuardNavigation (Compose NavHost)
            ├── HomeScreen ──── assess ──── SolGuardViewModel
            ├── DetailsScreen                    │
            └── SettingsScreen                   │
                                                 ├── SunPositionCalculator (NOAA math)
                                                 ├── UVIndexCalculator (UV math)
                                                 ├── SensorReader (light, compass, barometer)
                                                 └── UVDataRepository (JSON assets)
```

State is managed via `StateFlow<SessionState>` in the ViewModel. User preferences (skin type, SPF, surface) persist in `SharedPreferences`.

---

## 5. Project Structure

```
SolGuard/
├── docs/
│   └── APPROACH.md                              ← this file
├── android_app/
│   ├── build.gradle.kts                         # AGP + Kotlin plugin versions
│   ├── settings.gradle.kts                      # Project name: SolGuard
│   ├── gradle.properties
│   ├── gradlew
│   ├── gradle/wrapper/gradle-wrapper.properties
│   └── app/
│       ├── build.gradle.kts                     # Dependencies (Compose, Location, etc.)
│       ├── proguard-rules.pro
│       └── src/main/
│           ├── AndroidManifest.xml
│           ├── res/values/themes.xml
│           ├── assets/
│           │   ├── skin_types.json              # Fitzpatrick I-VI data
│           │   ├── uv_safety.json               # WHO UV categories + actions
│           │   └── ozone_factors.json           # Seasonal ozone correction table
│           └── java/com/solguard/app/
│               ├── MainActivity.kt
│               ├── session/SessionState.kt
│               ├── uv/
│               │   ├── SunPositionCalculator.kt # NOAA solar algorithm
│               │   ├── SensorReader.kt          # Light + compass + barometer
│               │   └── UVIndexCalculator.kt     # Core UV estimation
│               ├── data/
│               │   └── UVDataRepository.kt      # JSON asset reader
│               └── ui/
│                   ├── Navigation.kt            # NavHost with 3 screens
│                   ├── SolGuardViewModel.kt     # State management
│                   ├── theme/Theme.kt           # Material 3 sun-warning palette
│                   └── screens/
│                       ├── HomeScreen.kt        # UV gauge + recommendations
│                       ├── DetailsScreen.kt     # Technical breakdown
│                       └── SettingsScreen.kt    # Skin type, SPF, surface
```

---

## 6. How to Build the APK

### 6.1 Prerequisites

- **Android Studio** Ladybug (2024.2) or newer
- **JDK 17** (bundled with Android Studio)
- **Android SDK 35** (install via Android Studio → SDK Manager)
- No physical device required for building (but needed for testing sensors)

### 6.2 Clone the Repository

```bash
git clone <repo-url> SolGuard
cd SolGuard
```

### 6.3 Build via Android Studio (Recommended)

1. Open Android Studio
2. **File → Open** → select the `SolGuard/android_app/` folder
3. Wait for Gradle sync to complete (first time downloads ~500 MB of dependencies)
4. **Build → Build Bundle(s) / APK(s) → Build APK(s)**
5. APK will be at `android_app/app/build/outputs/apk/debug/app-debug.apk`

### 6.4 Build via Command Line

```bash
cd android_app

# Debug APK
./gradlew assembleDebug

# Release APK (requires signing config)
./gradlew assembleRelease
```

The debug APK is at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 7. How to Install on Android Phone

### 7.1 Via ADB (Developer Mode)

1. Enable **Developer Options** on your phone:
   - Go to **Settings → About Phone** → tap **Build Number** 7 times
2. Enable **USB Debugging** in Developer Options
3. Connect phone via USB cable
4. Run:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 7.2 Via File Transfer

1. Copy `app-debug.apk` to your phone (USB, email, Google Drive, etc.)
2. On the phone, open the APK file
3. If prompted, allow "Install from unknown sources" for your file manager
4. Tap **Install**

---

## 8. How to Use

### 8.1 First Launch

1. Open **SolGuard** from your app drawer
2. Grant **Location permission** when prompted (needed for sun position calculation)
3. Tap **"Check UV Index"**

### 8.2 Reading the Main Screen

- **UV Index gauge** — large number with color coding:
  - Green (0-2): Low
  - Yellow (3-5): Moderate
  - Orange (6-7): High
  - Red (8-10): Very High
  - Purple (11+): Extreme
- **Recommendation card** — what to do right now (shade, sunscreen, etc.)
- **Exposure card** — safe time without/with sunscreen, Vitamin D needs
- **UV curve** — today's UV profile showing peak hour
- **Sensor bar** — live lux, sun altitude, estimated altitude

### 8.3 Settings

Tap the gear icon to configure:
- **Skin type** (Fitzpatrick I-VI) — affects safe exposure calculations
- **SPF** (None, 15, 30, 50) — factors into sunscreen-adjusted exposure time
- **Surface type** (Grass, Sand, Water, Snow, City) — reflective surfaces increase UV

### 8.4 Technical Details

Tap **"Details"** button to see:
- Exact sun position (altitude, azimuth, zenith)
- Each correction factor applied (altitude, cloud, ozone, surface)
- Raw sensor readings (lux, compass heading, barometric pressure)
- GPS coordinates

---

## 9. Accuracy Notes

- UV estimates are **approximate** (±1-2 UV index points) due to:
  - Light sensor quality varies between phone models
  - Ozone data is seasonal average, not real-time
  - Air pollution (aerosols) is not accounted for
  - The barometric altitude formula assumes standard atmosphere
- For critical sun safety decisions (medical conditions, post-surgery), consult a dermatologist
- The app is most accurate at **mid-latitudes (15-45°)** where the ozone model is best calibrated
- Cloud factor works best when the phone's light sensor is exposed to open sky, not shaded

---

## 10. Future Enhancements

1. **Home screen widget** showing current UV index
2. **Weather API integration** (optional, for multi-day forecast)
3. **Sunscreen reminder notifications** with countdown timer
4. **Skin exposure log** tracking cumulative UV dose
5. **Wear OS companion** app
6. **AR sun path overlay** using camera + gyroscope
