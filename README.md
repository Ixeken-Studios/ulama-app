# Ulama - World Cup 2026 Companion




## A Android app for the **World Cup 2026** built with Kotlin and Jetpack Compose. Track fixtures, live scores, group standings and save your own match predictions.

[<img src="https://img.shields.io/badge/Download-Latest_Release-6A994E?style=for-the-badge&logo=android&logoColor=white" height="45">](https://github.com/Ixeken-Studios/ulama-app/releases/latest)
## Screenshots
![Screenshots](/fastlane/metadata/android/en-US/images/featureGraphic.png)
![Screenshots](/fastlane/metadata/android/en-US/images/featureGraphic_2.png)
## Key Features

### Match Center
- Date selector to browse the full tournament schedule.
- Live score.
- Match detail with timeline, statistics and your prediction.
- Activate notifications for future matches.

### Standings
- Group tables calculated from match results.

### Predictions
- Save score predictions for every match before kickoff.
- Penalty-winner prediction toggle for knockout rounds.
- Bento grid or list view with green/red border correction feedback.
- Progress dialog showing how many matches you have predicted.

### Favorites
- Mark your favorite teams and auto-schedule alarms for their upcoming matches.

### Settings
- Configurable pre-match alert (5 to 60 minutes).
- Kickoff notification toggle.
- ESPN live data toggle.
- Permissions dashboard (notifications, exact alarms, battery optimization).

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.2 |
| UI | Jetpack Compose, Material 3 |
| Architecture | Clean Architecture, MVVM, Unidirectional Data Flow |
| DI | Hilt 2.59 |
| Database | Room 2.8 |
| Network | Retrofit (openfootball fixture + ESPN live scores) |
| Background | WorkManager (periodic sync), AlarmManager (match reminders) |
| Build | AGP 9.2, KSP, Gradle with Configuration Cache |

<p align="center">
  <img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/Android/android2.svg">&nbsp;&nbsp;
  <img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/Kotlin/kotlin2.svg">&nbsp;&nbsp;
  <img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white">&nbsp;&nbsp;
  <img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/SQLite/sqlite2.svg">&nbsp;&nbsp;

</p>

## Requirements

- Android Studio Meerkat (2025.2) or later
- JDK 17+ (Android Studio bundled JBR recommended)
- Android SDK with platform API 35 installed
- A device or emulator running **Android 14 (API 34)** or higher

## Quick Start

1. Clone the repository

```bash
git clone https://github.com/Ixeken-Studios/ulama-app.git
cd ulama-app
```

2. Create or verify `local.properties` with your SDK path (this file is git-ignored).

3. Build the project

```bash
./gradlew assembleDebug
```

4. Run on a connected device or emulator from Android Studio.

## Project Structure

```
com.ixeken.worldcupinfo/
├── data/          # Room database, DAOs, entities, mappers, Retrofit services
├── di/            # Hilt modules (Database, Network)
├── domain/        # Pure models (Match, Prediction), repository interfaces, use cases
├── notification/  # AlarmManager scheduler and BroadcastReceiver
├── ui/            # Compose screens (Calendar, Groups, Favorites, Predictions, Settings)
├── worker/        # WorkManager sync worker
├── MainActivity.kt
└── WorldCupApplication.kt
```

## Notes

- All user-facing strings are localized in English and Spanish via XML string resources.
- Notification preferences are stored in `SharedPreferences` under the key `alarm_minutes`.
- Do not commit `local.properties`, `*.jks` keystores or `secrets.properties`.

## Contributing

Open issues or pull requests. All UI text must use Android string resources with both `en` and `es` translations.

## License
  <img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/LicenceMIT/licencemit2.svg">


