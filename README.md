# Efina Android

Native Android application for Efina personal finance management.

## Features

- **Account Management**: Track bank accounts, e-wallets, cash, credit cards
- **Transaction Tracking**: Record income, expenses, and transfers
- **Budgeting**: Set monthly, weekly, or yearly budgets per category
- **Savings Goals**: Track progress toward financial goals
- **Gamification**: Earn XP, unlock achievements, compete on leaderboards
- **AI Assistant**: Get personalized insights and recommendations
- **Receipt OCR**: Scan receipts to auto-categorize transactions
- **Offline Support**: Work without internet with background sync

## Tech Stack

| Category | Library |
|----------|---------|
| UI | Jetpack Compose + Material 3 |
| DI | Hilt |
| Networking | Retrofit + OkHttp |
| Local DB | Room |
| Preferences | DataStore |
| Navigation | Navigation Compose |
| Background | WorkManager |
| Auth | EncryptedSharedPreferences |

## API

The app connects to the Efina backend API. See [API Documentation](../docs/mobile-api-spec.md) for details.

## Getting Started

### Prerequisites

- Android Studio Hedgehog or later
- Kotlin 1.9+
- JDK 17

### Setup

1. Clone the repository:
```bash
git clone https://github.com/YOUR_USERNAME/efina-android.git
```

2. Open in Android Studio and sync Gradle

3. Create `keystore.properties` for release signing:
```properties
storePassword=your_store_password
keyPassword=your_key_password
keyAlias=your_key_alias
storeFile=your_keystore.jks
```

4. Build and run:
```bash
./gradlew assembleDebug
```

## Architecture

Clean Architecture with MVVM:

```
app/src/main/java/com/efina/app/
├── di/                 # Hilt dependency injection modules
├── data/
│   ├── remote/         # Retrofit API, DTOs, interceptors
│   └── repository/    # Repository implementations
├── domain/
│   ├── model/         # Domain models
│   └── repository/     # Repository interfaces
├── ui/                 # Compose screens and ViewModels
└── util/               # Utilities and constants
```

## Implementation Plan

See [Implementation Plan](../docs/android-implementation-plan.md) for full task breakdown.

## License

MIT
