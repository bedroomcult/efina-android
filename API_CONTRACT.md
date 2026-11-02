# API Contract — efina-android

Every route the Android client calls, verified against the backend source by
`tools/check-api-contract.mjs`. CI runs it in `--strict` mode against a committed snapshot of the backend routes
(`tools/backend-routes.json`), because the backend repository is private and
cannot be cloned by CI. Regenerate the snapshot with `--emit-manifest`.

```
node tools/check-api-contract.mjs --strict   # exit 0
```

Backend routing facts this document depends on:

- `functions/api/<x>/index.js` serves `/api/<x>`; `[id].js` serves `/api/<x>/{id}`.
- A param *directory* (`accounts/[id]/members.js`) serves `/api/<x>/{id}/members`.
- Handlers export `onRequestGet|Post|Put|Patch|Delete`, in either
  `export async function` or `export const … = async` form. **A missing export is
  a 405 at runtime.**
- `_middleware.js` validates the session id inside the JWT against the `sessions`
  table on every request, and enforces CSRF on POST/PUT/PATCH/DELETE.

Base URL: `https://efina.himz.my.id/api/` (was `efina.11112021.xyz`, which is
dead — a `curl` to it returns 000, while the new host returns 200).

---

## Defects found in the previous implementation

The original `EfinaApi.kt` was written against assumed routes. Nine did not exist
or used the wrong verb. All were confirmed by reading `functions/api/`.

| # | Kotlin claimed | Reality | Consequence |
|---|---|---|---|
| 1 | `GET auth/me` | **no such route** | current-user call always 404 |
| 2 | `POST auth/logout` | `DELETE auth/sessions` | logout never worked |
| 3 | `GET dashboard` | **no such route** | dashboard always 404 |
| 4 | `GET ai/xp-summary` | **no such route** | XP summary always 404 |
| 5 | `PATCH accounts/{id}` | `PUT accounts/{id}` (PATCH exists, sharing only) | update silently 405 |
| 6 | `PATCH transactions/{id}` | `PUT transactions/{id}` | update silently 405 |
| 7 | `PATCH budgets/{id}` | `PUT budgets/{id}` | update silently 405 |
| 8 | `PATCH goals/{id}` | `PUT goals/{id}` | update silently 405 |
| 9 | `PATCH categories/{id}` | `PUT categories/{id}` | update silently 405 |
| 10 | `PATCH settings` | `PUT settings` | settings never saved |

Additional shape mismatches corrected:

- `GET transactions` returns `{ transactions, total, limit, offset }`, not an array.
- `accounts.type` values are `Cash | Bank | E-Wallet | Credit | Other`; the old
  enum used `checking | savings | investment | credit | cash | other`. Only
  `Cash`, `Bank` and `E-Wallet` are named in the backend
  (`functions/api/health-score/index.js:209`); the rest are accepted as free text.
- `categories` **has** a `type` column and `POST categories` requires it
  (`functions/api/categories/index.js:172,193`). The seeded defaults are all
  `expense`. `Category.type` is sent on create.
- `POST transfers` returns `{ transfer_pair_id, from_transaction, to_transaction }`,
  not a single `Transaction`.

---

## Routes in use

### Auth

| Method | Path | Body | Response |
|---|---|---|---|
| POST | `auth/login` | `{email,password}` | `{token, csrfToken, user}` |
| POST | `auth/register` | `{email,password,name?}` | `{token, csrfToken, user}` |
| DELETE | `auth/sessions` | — | session row invalidated |
| POST | `auth/google` | `{credential}` | `{token, csrfToken, user}` |
| POST | `auth/reset-password/request` | `{email}` | — |

Login is exempt from CSRF (public route list in `_middleware.js`). Every other
call sends `Authorization: Bearer` **and** `X-CSRF-Token` on state-changing verbs.

### User & settings

| Method | Path | Notes |
|---|---|---|
| GET | `user` | full profile; replaces the nonexistent `auth/me` |
| PUT | `user` | `{name?, force_onboarding?}` |
| POST | `user/change-password` | `{oldPassword, newPassword}` — **POST**, not PUT |
| GET | `settings` | settings row |
| PUT | `settings` | partial body, COALESCEd server-side |
| GET | `user/heatmap` | contribution heatmap |
| GET | `user/monthly-stats` | monthly aggregates |

### Accounts

| Method | Path | Notes |
|---|---|---|
| GET | `accounts` | array incl. shared accounts; each row has `account_role` |
| GET | `accounts/{id}` | |
| POST | `accounts` | `{name,type,balance,currency,description,color,family_id}` |
| PUT | `accounts/{id}` | `{name,type,balance,currency,description,color}` |
| PATCH | `accounts/{id}` | sharing toggle only: `{family_id}` |
| DELETE | `accounts/{id}` | owner only |
| GET | `accounts/recalculate` | reconciliation report |
| GET | `accounts/{id}/members` | `{account_role, family_id, is_shared, owner, members}` |
| POST | `accounts/{id}/members` | `{userId, role, family_id}` |
| DELETE | `accounts/{id}/members` | `{userId}` |

`POST accounts/{id}/members` rejects anyone not already in the same family.

### Transactions

| Method | Path | Notes |
|---|---|---|
| GET | `transactions` | **envelope**; query: limit, offset, type, account_id, category_id, start_date, end_date, search, include_subcategories |
| POST | `transactions` | `{account_id,category_id?,amount,type,description?,date,mood?,source}` |
| PUT | `transactions/{id}` | full body |
| DELETE | `transactions/{id}` | |
| POST | `transfers` | `{from_account_id,to_account_id,amount,date,description?}` |
| PUT | `transfers/{id}` | |
| DELETE | `transfers/{id}` | |

`account_id` is required on both create and update. A transfer is **not** a
transaction with `type=transfer`; it is a separate endpoint that writes a linked
pair and moves both balances atomically.

### Categories, budgets, goals

| Method | Path |
|---|---|
| GET/POST | `categories`, `budgets`, `goals` |
| PUT/DELETE | `categories/{id}`, `budgets/{id}`, `goals/{id}` |
| POST | `goals/{id}/add-funds` (body `{amount, account_id}`) |
| GET | `budgets/vs-actual` |

### Debts, scheduled, notifications

| Method | Path |
|---|---|
| GET/POST | `debts`, `scheduled` |
| PUT/DELETE | `debts/{id}`, `scheduled/{id}` |
| POST | `debts/{id}/pay` |
| GET | `notifications` |
| DELETE | `notifications/{id}` |
| PUT | `notifications/read-all` — **PUT**, not POST |

### Insights

| Method | Path | Notes |
|---|---|---|
| GET | `achievements` | array |
| GET | `gamification/history` | XP ledger; replaces `ai/xp-summary` |
| GET | `leaderboard` | query: limit, offset, sort_by |
| GET | `forecast` | |
| GET | `health-score` | |
| POST | `ai/chat` | `{message, history}` |
| GET | `ai/context` | |

### Family

| Method | Path |
|---|---|
| GET/POST | `families` |
| GET | `families/{id}` |
| POST/DELETE | `families/{id}/members` |
| POST | `families/invites/accept` |

Free plan is capped at one family (`FAMILY_LIMIT_REACHED`); invite codes are 8
characters and expire after 72 hours.

---

## Deliberately not called

| Backend area | Why not |
|---|---|
| `admin/*` | operator-only; the app has no admin surface |
| `mcp/*` | MCP server + OAuth, not an app feature |
| `sales/*`, `products/*` | separate business-product module |
| `telegram/*` | webhook ingress |
| `public/*` | diagnostics, not user data |
| `auth/pin`, `auth/verify-pin`, `auth/reset-pin` | PIN lock is a device feature; not wired yet, listed in PARITY.md |
