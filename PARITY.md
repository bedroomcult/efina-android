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

## API audit — the diff against the original EfinaApi.kt

Six wrong verbs and three nonexistent paths, each found by reading
`functions/api/` in the web repo. Full request/response detail is in
`API_CONTRACT.md`; this is the diff the audit task requires.

| # | Kotlin originally declared | Backend actually has | Traceable to |
|---|---|---|---|
| 1 | `GET auth/me` | **no such route** — current user is `GET user` | `functions/api/user/index.js` |
| 2 | `POST auth/logout` | **no such route** — it is `DELETE auth/sessions` | `functions/api/auth/sessions.js` |
| 3 | `GET dashboard` | **no such route** — composed client-side from accounts + transactions + budgets + goals | `functions/api/` (no dashboard dir) |
| 4 | `PATCH accounts/{id}` | `PUT accounts/{id}` (PATCH exists but is the sharing toggle only) | `functions/api/accounts/[id].js` |
| 5 | `PATCH transactions/{id}` | `PUT transactions/{id}` | `functions/api/transactions/[id].js` |
| 6 | `PATCH budgets/{id}` | `PUT budgets/{id}` | `functions/api/budgets/[id].js` |
| 7 | `PATCH goals/{id}` | `PUT goals/{id}` | `functions/api/goals/[id].js` |
| 8 | `PATCH categories/{id}` | `PUT categories/{id}` | `functions/api/categories/[id].js` |
| 9 | `PATCH settings` | `PUT settings` | `functions/api/settings/index.js` |
| 10 | `GET ai/xp-summary` | **no such route** — XP history is `gamification/history` | `functions/api/gamification/history.js` |

Rows 1–3 and 10 are the nonexistent paths; rows 4–9 are the six wrong verbs.
`API_BASE_URL` also pointed at `efina.11112021.xyz` (curl returns 000) instead of
`efina.himz.my.id` (curl returns 200).

## Every endpoint under functions/api/

All 138 routes, extracted from the backend source by tools/check-api-contract.mjs --emit-manifest. Auth and CSRF apply to every one except the public routes listed in _middleware.js.

**accounts**

- DELETE /api/accounts/:id
- DELETE /api/accounts/:id/members
- GET /api/accounts
- GET /api/accounts/:id
- GET /api/accounts/:id/members
- GET /api/accounts/recalculate
- PATCH /api/accounts/:id
- POST /api/accounts
- POST /api/accounts/:id/members
- POST /api/accounts/recalculate
- PUT /api/accounts
- PUT /api/accounts/:id

**achievements**

- GET /api/achievements
- POST /api/achievements/:id/seen
- POST /api/achievements/batch-seen

**admin**

- DELETE /api/admin/achievements
- DELETE /api/admin/pricing/:id
- DELETE /api/admin/user-achievements
- DELETE /api/admin/users
- GET /api/admin/achievements
- GET /api/admin/gamification-details
- GET /api/admin/pricing
- GET /api/admin/users
- PATCH /api/admin/achievements
- PATCH /api/admin/users
- POST /api/admin/achievements
- POST /api/admin/backfill-user
- POST /api/admin/pricing
- POST /api/admin/recalc-tiers
- POST /api/admin/user-achievements
- PUT /api/admin/pricing/:id

**ai**

- GET /api/ai/context
- GET /api/ai/mood_insights
- POST /api/ai/chat
- POST /api/ai/metadata
- POST /api/ai/mood_insights
- POST /api/ai/ocr

**auth**

- DELETE /api/auth/sessions
- GET /api/auth/csrf
- GET /api/auth/sessions
- POST /api/auth/google
- POST /api/auth/login
- POST /api/auth/register
- POST /api/auth/reset-password/confirm
- POST /api/auth/reset-password/request
- POST /api/auth/reset-pin
- POST /api/auth/verify-pin
- PUT /api/auth/pin

**budgets**

- DELETE /api/budgets/:id
- GET /api/budgets
- GET /api/budgets/vs-actual
- POST /api/budgets
- POST /api/budgets/check-alerts
- PUT /api/budgets/:id

**categories**

- DELETE /api/categories/:id
- GET /api/categories
- POST /api/categories
- PUT /api/categories/:id

**debts**

- DELETE /api/debts/:id
- GET /api/debts
- GET /api/debts/:id
- POST /api/debts
- POST /api/debts/:id/pay
- POST /api/debts/check-overdue
- PUT /api/debts/:id

**families**

- DELETE /api/families/:id/members
- GET /api/families
- GET /api/families/:id
- POST /api/families
- POST /api/families/:id/members
- POST /api/families/invites/accept

**forecast**

- GET /api/forecast

**gamification**

- GET /api/gamification/history
- POST /api/gamification/award-xp

**goals**

- DELETE /api/goals/:id
- GET /api/goals
- POST /api/goals
- POST /api/goals/:id/add-funds
- PUT /api/goals/:id

**health-score**

- GET /api/health-score

**leaderboard**

- GET /api/leaderboard

**mcp**

- DELETE /api/mcp/keys
- GET /api/mcp/keys
- PATCH /api/mcp/keys
- POST /api/mcp/keys

**notifications**

- DELETE /api/notifications
- DELETE /api/notifications/:id
- GET /api/notifications
- POST /api/notifications
- PUT /api/notifications/read-all

**products**

- DELETE /api/products/:id
- GET /api/products
- POST /api/products
- PUT /api/products/:id

**public**

- GET /api/public/debug
- GET /api/public/pricing
- POST /api/public/debug

**sales**

- DELETE /api/sales/:id
- GET /api/sales
- GET /api/sales/:id
- GET /api/sales/analytics
- POST /api/sales
- PUT /api/sales/:id
- PUT /api/sales/:id/status

**scheduled**

- DELETE /api/scheduled/:id
- GET /api/scheduled
- POST /api/scheduled
- POST /api/scheduled/:id/confirm
- POST /api/scheduled/:id/pay-now
- POST /api/scheduled/:id/skip
- POST /api/scheduled/process
- PUT /api/scheduled/:id

**settings**

- GET /api/settings
- PUT /api/settings

**telegram**

- DELETE /api/telegram/link
- GET /api/telegram/link
- POST /api/telegram/link

**transactions**

- DELETE /api/transactions/:id
- GET /api/transactions
- GET /api/transactions/anomalies
- POST /api/transactions
- POST /api/transactions/:id/split
- PUT /api/transactions/:id

**transfers**

- DELETE /api/transfers/:id
- POST /api/transfers
- PUT /api/transfers/:id

**user**

- DELETE /api/user
- GET /api/user
- GET /api/user/export
- GET /api/user/heatmap
- GET /api/user/monthly-stats
- GET /api/user/stats
- POST /api/user/backfill-achievements
- POST /api/user/bind-google
- POST /api/user/change-password
- POST /api/user/refresh-achievements
- POST /api/user/review-summary
- PUT /api/user

---

## Design parity

See **DESIGN-PARITY.md** — a section-by-section table mapping design.md to the
Kotlin file implementing it, with every omission named and the two
compiler-verified impossibilities recorded.

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
| Compile | CI `./gradlew assembleDebug` | **success** — 22.1 MB debug APK, green across three runs |
| Signed release | `.github/workflows/release.yml` on a `v*` tag | not yet exercised; needs `ANDROID_KEYSTORE_*` secrets |

The static checks cover bracket balance, project-import resolution, and absence of
placeholder markers. They do **not** type-check. Only Gradle does that.

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
