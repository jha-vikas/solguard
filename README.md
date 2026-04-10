# SolGuard

**Offline UV index estimator and sun safety timer for Android.**

SolGuard uses your phone's built-in sensors (GPS, ambient light, barometer, compass) to estimate the current UV index and tell you how long you can safely stay in the sun. No internet connection required -- everything runs on-device.

## Features

- **Real-time UV Index** -- estimated from solar position, altitude, cloud cover, and seasonal ozone data
- **Safe Exposure Timer** -- how many minutes until sunburn, based on your skin type and sunscreen SPF
- **Vitamin D Timer** -- minimum sun exposure needed for daily Vitamin D (1000 IU)
- **Daily UV Curve** -- today's hour-by-hour UV forecast showing peak time and safe windows
- **WHO Safety Guidance** -- color-coded categories (Low / Moderate / High / Very High / Extreme) with actionable recommendations
- **Technical Details View** -- full breakdown of sun position, correction factors, and raw sensor readings
- **Fully Offline** -- no API calls, no cloud, no tracking. Works in airplane mode.

## How It Works

```
GPS + Clock  ──►  NOAA Sun Position  ──►  Solar Zenith Angle
                                               │
Light Sensor ──►  Cloud Cover Proxy  ──────────┤
                                               │
Barometer    ──►  Altitude (meters)  ──────────┤
                                               ▼
                                      UV Index = 12 × cos(SZA)^2.4
                                                × altitude correction
                                                × cloud factor
                                                × ozone seasonal factor
                                                × surface reflection
```

See [docs/APPROACH.md](docs/APPROACH.md) for the full technical approach, algorithm details, and accuracy notes.

## Screenshots

*Coming soon -- build the APK and try it on your phone.*

## Quick Start

### Prerequisites

- Android Studio Ladybug (2024.2) or newer
- JDK 17 (bundled with Android Studio)
- Android SDK 35
- An Android phone (API 26+ / Android 8.0+) for testing sensors

### Build

```bash
# Clone
git clone <repo-url> SolGuard
cd SolGuard/android_app

# Build debug APK
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

Or open `SolGuard/android_app/` in Android Studio and click **Build > Build APK(s)**.

### Install

```bash
# Via ADB
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or copy the APK to your phone and open it directly.

### First Launch

1. Grant location permission when prompted
2. Tap **"Check UV Index"**
3. Open **Settings** (gear icon) to set your skin type and sunscreen SPF

## Project Structure

```
SolGuard/
├── docs/APPROACH.md              # Full technical approach and build guide
├── android_app/
│   ├── build.gradle.kts          # Root Gradle config
│   ├── settings.gradle.kts
│   └── app/
│       ├── build.gradle.kts      # App dependencies (Compose, Location, etc.)
│       └── src/main/
│           ├── assets/            # JSON knowledge bases (skin types, UV safety, ozone)
│           └── java/com/solguard/app/
│               ├── uv/           # Sun position (NOAA), sensors, UV calculator
│               ├── data/         # JSON asset reader
│               ├── session/      # App state
│               └── ui/           # Compose screens (Home, Details, Settings)
```

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navigation | Compose Navigation |
| Sun Position | NOAA Solar Calculator (pure math) |
| UV Model | Beer-Lambert empirical fit |
| Sensors | Android SensorManager (light, barometer, compass) |
| Location | Google Play Services FusedLocationProvider |
| Data | JSON assets, SharedPreferences |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 |

## Accuracy

UV estimates are approximate (within +-1-2 UV index points). Factors that affect accuracy:

- Light sensor quality varies across phone models
- Ozone data uses seasonal averages, not real-time satellite data
- Air pollution and aerosols are not modeled
- Barometric altitude assumes standard atmosphere

For medical decisions, consult a dermatologist. See [docs/APPROACH.md](docs/APPROACH.md) for detailed accuracy notes.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

## Acknowledgments

- Sun position algorithm based on the [NOAA Solar Calculator](https://gml.noaa.gov/grad/solcalc/)
- UV index categorization follows [WHO UV Index guidelines](https://www.who.int/news-room/questions-and-answers/item/radiation-the-ultraviolet-(uv)-index)
- Skin type classification uses the [Fitzpatrick scale](https://en.wikipedia.org/wiki/Fitzpatrick_scale)
- Sensor fusion approach adapted from the [Sunleaf](https://github.com/user/sunleaf) plant care app
