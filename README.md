# Efina Android

Native Android client for the Efina personal finance app.

Jetpack Compose, Kotlin 2.0, Hilt, Retrofit. Talks to the existing Efina
Cloudflare Pages API — no separate backend.

---

## Status

| | |
|---|---|
| CI | [![Build](https://github.com/bedroomcult/efina-android/actions/workflows/build.yml/badge.svg)](https://github.com/bedroomcult/efina-android/actions/workflows/build.yml) |
| Compiles | yes — debug and signed release both build in CI |
| Has it been run? | **no** — verified by compiler only, never executed on a device |

That last row matters more than the others. Everything below is proven by
compilation and by static checks against the real backend; nothing is proven by
runtime behaviour. Read `PARITY.md` before assuming a feature works.

## What is implemented

Sign-in and register, session restore, dashboard, accounts (create/edit/delete
and sharing with family members), transactions (create/edit/delete plus
transfers), categories, budgets, goals, statistics, settings, family, debts and
scheduled transactions.

`PARITY.md` lists every web feature that is **not** implemented, with a reason.
`DESIGN-PARITY.md` maps each section of `design.md` to the Kotlin file that
implements it, and names what was deliberately skipped.

## API

The client is matched to the real backend, not to an assumed one.
`API_CONTRACT.md` records every route the client uses, with method, path, body
and response; `PARITY.md` lists all 138 backend routes.
and the ten defects in the previous API layer that made most screens fail.

```
https://efina.himz.my.id/api/
```

Base URL is a `buildConfigField` in `app/build.gradle.kts`, set per build type.

## Build

Requires JDK 17 and the Android SDK (compileSdk 35).

```bash
./gradlew assembleDebug          # debug APK
./gradlew assembleRelease        # unsigned unless keystore.properties exists
```

Signed releases come from `.github/workflows/release.yml` on a `v*` tag, which
reads `ANDROID_KEYSTORE_CONTENT`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`
and `ANDROID_KEY_PASSWORD` from repository secrets.

## Verification

Neither check needs Gradle, so both run anywhere:

```bash
node tools/check-api-contract.mjs --strict   # every route exists on the backend
node tools/check-kotlin.mjs --strict         # bracket balance, imports, no stubs
```

Both gate CI. The first also regenerates the route snapshot:

```bash
node tools/check-api-contract.mjs --emit-manifest   # rewrites tools/backend-routes.json
```

That snapshot exists because the Efina backend repository is private and CI
cannot clone it.

## Crash reports

Uncaught exceptions are posted to `POST /api/public/debug`, the endpoint the web
client already uses, and are readable at `GET /api/public/debug` with a 7-day
retention. Each report carries the thread, device model, Android version, app
version and full stack.

## Project layout

```
app/src/main/java/com/efina/app/
├── data/
│   ├── remote/api/         Retrofit interface, one method per real route
│   ├── remote/dto/         wire models + mappers (faithful to D1 column names)
│   ├── remote/interceptor/ auth + CSRF, encrypted token storage
│   └── repository/         one repository per feature
├── domain/model/           UI-facing models
├── domain/repository/      contracts
├── di/                     Hilt modules
├── ui/                     feature packages (screens + viewmodels)
│   └── theme/              palette, type, shape, motion
└── util/                   constants, preferences, crash reporter
```

## License

MIT
