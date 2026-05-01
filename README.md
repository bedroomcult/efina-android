# Efina Android

Native Android application for Efina personal finance management.

## Quick Start

1. **Install Gradle wrapper JAR:**
   ```bash
   # Download from https://github.com/gradle/gradle/raw/v8.4.0/gradle/wrapper/gradle-wrapper.jar
   # Or clone from official Android project template
   
   # After download:
   curl -L -o gradle/wrapper/gradle-wrapper.jar \
     "https://github.com/gradle/gradle/raw/v8.4.0/gradle/wrapper/gradle-wrapper.jar"
   ```

2. **Open in Android Studio** (Hedgehog or later)

3. **Sync Gradle** and wait for dependencies

4. **Run:**
   ```bash
   ./gradlew assembleDebug
   ```

## Features

- Account Management (bank, e-wallet, cash, credit, investment)
- Transaction Tracking (income, expense, transfer)
- Budgeting (weekly, monthly, yearly)
- Savings Goals with progress tracking
- Gamification (XP, achievements, leaderboard)
- AI Assistant with chat
- Receipt OCR scanning
- Offline support with Room database

## Tech Stack

| Component | Library |
|-----------|---------|
| UI | Jetpack Compose + Material 3 |
| DI | Hilt 2.50 |
| Networking | Retrofit 2.9 + OkHttp 4.12 |
| Local DB | Room 2.6.1 |
| Preferences | DataStore |
| Navigation | Navigation Compose 2.7.7 |
| Background | WorkManager 2.9 |
| Auth | EncryptedSharedPreferences |

## Project Structure

```
app/src/main/java/com/efina/app/
├── data/
│   ├── remote/
│   │   ├── api/         # Retrofit interfaces
│   │   ├── dto/         # API response models
│   │   └── interceptor/  # Auth interceptors
│   └── repository/      # Repository implementations
├── domain/
│   ├── model/          # Domain models
│   └── repository/      # Repository interfaces
├── di/                 # Hilt modules
├── ui/                  # Compose screens + ViewModels
│   ├── auth/
│   ├── accounts/
│   ├── transactions/
│   ├── budgets/
│   ├── goals/
│   ├── stats/
│   ├── chat/
│   ├── settings/
│   ├── components/
│   └── theme/
└── util/               # Utilities, constants
```

## API Connection

The app connects to the Efina backend. Configure the API URL in `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://your-api-url.com/\"")
```

See [docs/mobile-api-spec.md](docs/mobile-api-spec.md) for full API documentation.

## Setup Requirements

- Android Studio Hedgehog+
- Kotlin 1.9+
- JDK 17
- Gradle 8.4 (wrapper included)

## Implementation Status

| Phase | Status |
|-------|--------|
| 1. Project Foundation | ✅ |
| 2. Authentication | 🔄 |
| 3-6. Core Features | ⏳ |
| 7. AI Features | ⏳ |
| 8. Settings/Polish | ⏳ |
| 9. Offline/Sync | ⏳ |
| 10. Release | ⏳ |

Full plan: [docs/android-implementation-plan.md](docs/android-implementation-plan.md)

## License

MIT
