# PARITY.md — efina-android vs efina (web)

What is implemented natively, what is not, and why. No feature is marked done
unless a screen **and** a working API call exist for it.

Generated for the rebuild described in API_CONTRACT.md. Static checks:
`node tools/check-kotlin.mjs --strict` and
`node tools/check-api-contract.mjs --strict`.

---

## Implemented

| Area | Android | Backend routes | Notes |
|---|---|---|---|
| Sign in | `LoginScreen` | `POST auth/login` | email + password, inline register toggle |
| Register | `LoginScreen` (register mode) | `POST auth/register` | enforces the same 6-char minimum as the server |
| Session restore | `AuthViewModel.restore()` | `GET user` | renders from the encrypted token immediately, refreshes in background |
| Logout | `SettingsScreen` | `DELETE auth/sessions` | clears encrypted storage regardless of network result |
| Dashboard | `HomeScreen` | composed from `accounts` + `transactions` + `budgets` + `goals` + `health-score` | there is no `/api/dashboard`; fanned out in parallel |
| Accounts list | `AccountsScreen` | `GET accounts` | shows shared accounts with role badges |
| Account create/edit | `AccountFormScreen` | `POST accounts`, `PUT accounts/{id}` | one form, two verbs |
| Account delete | `AccountsScreen` | `DELETE accounts/{id}` | owner only, confirmed |
| Account sharing | `AccountSharingScreen` | `GET/POST/DELETE accounts/{id}/members`, `PATCH accounts/{id}` | role picker; only family members can be added |
| Transactions list | `TransactionsScreen` | `GET transactions` | envelope + pagination + type filter + search |
| Transaction create/edit/delete | `TransactionFormScreen` | `POST`, `PUT`, `DELETE transactions/{id}` | |
| Transfers | `TransactionFormScreen` | `POST transfers` | routed to the transfer endpoint, not `type=transfer` |
| Categories | `CategoriesScreen` | `GET/POST/PUT/DELETE categories` | no income/expense typing, matching the schema |
| Budgets | `BudgetsScreen` | `GET/POST/PUT/DELETE budgets` | progress, over-limit warning, create dialog |
| Goals | `GoalsScreen` | `GET/POST/PUT/DELETE goals`, `POST goals/{id}/add-funds` | fund-from-account flow |
| Statistics | `StatsScreen` | `GET transactions` + `forecast` + `health-score` + `achievements` + `leaderboard` | derived client-side |
| Settings | `SettingsScreen` | `GET/PUT settings`, `POST user/change-password` | currency, language, theme, password, sign out |
| Family | `FamilyScreen` | `families`, `families/{id}`, `families/{id}/members`, `families/invites/accept` | invite code + 72h expiry surfaced |
| Debts | `DebtsScreen` | `GET/POST/PUT/DELETE debts`, `POST debts/{id}/pay` | borrow vs lend, payment books against an account |
| Scheduled | `ScheduledScreen` | `GET/POST/DELETE scheduled` | recurring create + cancel |

---

## Not implemented

Each row names the reason so nothing is silently missing.

| Web feature | Why not | What it needs |
|---|---|---|
| Scheduled toggle/pause | `ScheduledScreen` can create and delete; the backend has an `is_active` flag with no PATCH wired | expose update, not create/delete only |
| AI Assistant chat | `ChatRepository` + `POST ai/chat` wired | a chat screen with history state |
| Receipt OCR | native camera + `POST ai/ocr` multipart | camera permission flow, file upload |
| Leaderboard screen | data loads inside Stats | dedicated screen if it warrants one |
| Achievements screen | listed inside Stats | dedicated screen |
| Onboarding wizard | web has a 3-step flow (`Onboarding.jsx`) | mirrored wizard; currently first-run lands on the dashboard |
| PIN lock | backend has `auth/pin`, `verify-pin`, `reset-pin` | device biometric/PIN UX |
| Google sign-in | `POST auth/google` is in the API layer | Google Play Services dependency + consent screen config |
| Password reset | `POST auth/reset-password/request` is in the API layer | two screens (request, confirm) |
| Sales / products module | different product surface | product decision |
| Admin dashboard | operator-only | not an app feature |
| Profile page (gamification detail) | Stats shows the summary | level/XP/tier detail screen |
| PWA install prompt | not applicable | — |
| Offline cache | Room is declared but unused | see "Known gaps" |

---

## Design parity (design.md)

| design.md section | Implementation | Status |
|---|---|---|
| §2.1 Theme modes | `Theme.kt` | Light + Dark + Mono + Material You. **Dusk and OLED omitted** — `dynamicColorScheme` covers most of the intent on Android 12+ |
| §2.2 14 palettes | `Color.kt` | **2 of 14** (Orion, Mono). Token names match, so adding one is a new object |
| §2.3 Brand palette | `OrionLight` / `OrionDark` | Full primary/secondary/tertiary/surface/error set |
| §2.4 Semantic colours | `EfinaSemantic` | safe/caution/danger/warn/info |
| §2.5 `tonal()` | `tonal()` in `Color.kt` | Same HSL construction, 0.88/0.26 containers, 0.30/0.82 foreground |
| §2.6 Install-state colours | n/a | App-store states have no finance analogue; income/expense/transfer get `MoneyColors` instead |
| §3 Typography | `Type.kt` | Full M3 scale + `EfinaType` emphasized variants. **Font is the platform default, not Space Grotesk** — avoids a first-frame flash and a font download |
| §4 Corner scale | `Shape.kt` | 4/8/12/16/28 + `EfinaCorners` for hero (28), menu (20), grouped rows (20/4 with 2dp gaps), pill, icon (24%) |
| §4 Segmented lists | `SegmentedColumn`/`SegmentedRow`, used in Accounts and Settings | |
| §4 Shape morphing | **omitted** | Requires `MaterialShapes` (material3 1.4+); the BOM here predates it |
| §5 Motion scheme | `Motion.kt` `EfinaMotion` | spring specs approximating `MotionScheme.expressive()`. **Not the real API** — it does not exist in this Compose BOM |
| §5 `springClick` | `Modifier.springClick` | 0.94 press scale; haptic hook present but not fired (no haptics dependency) |
| §5 `reveal(index)` | `revealState(index)` | 28dp rise + fade, 45ms stagger, first 9 items |
| §5 Shared-element icons | **omitted** | `SharedTransitionLayout` is also 1.4+ |
| §5 Hero pager auto-advance | n/a | no carousel in a finance app |
| §5 Dock / pull-to-refresh | `PullToRefreshBox` on Home | Standard M3 indicator, not the mascot |
| §5 Skeletons | `SkeletonRow`, `LoadingList` | |

---

## Known gaps and risks

1. **No offline layer.** Room is in `build.gradle.kts` but nothing uses it. Every
   screen is network-only. The web app has react-query persistence; Android does
   not, so a dead connection shows an error instead of cached data.

2. **No token refresh.** The backend issues a 7-day session with no refresh
   endpoint. When it expires the app logs the user out rather than renewing.

3. **`X-CSRF-Token` is read from the login response only.** If the server rotates
   the CSRF cookie mid-session the app will 403 until re-login.

4. **Currency is resolved by joining the accounts list.** The `GET transactions`
   response does not include currency, so `TransactionsViewModel` builds an
   `accountId -> currency` map from `GET accounts` and applies it per row. Two
   requests on first load instead of one; correct for multi-currency users.

5. **Compiles, but never run.** CI `assembleDebug` succeeds and produces a 22 MB
   APK, which proves the code type-checks and links. No emulator or device has
   executed it, so runtime behaviour (navigation, network, layout) is unproven.

6. **Compose BOM is 2024.02.00.** Several design.md APIs (Material 3 Expressive,
   `MaterialShapes`, `SharedTransitionLayout`) require newer versions. Bumping the
   BOM is the single highest-value follow-up for design fidelity.

---

## Verification

| Check | Command | Result |
|---|---|---|
| API contract | `node tools/check-api-contract.mjs --strict` | exit 0 |
| Kotlin static | `node tools/check-kotlin.mjs --strict` | exit 0 |
| Compile | CI `./gradlew assembleDebug` | **success** — 22.1 MB debug APK, twice (also after debts/scheduled) |

The static checks cover bracket balance, project-import resolution, and absence of
placeholder markers. They do **not** type-check. Only Gradle does that.
