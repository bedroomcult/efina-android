# Offline sync

The app keeps a local mirror of the ledger and works without a connection. This
document records the design and, at the bottom, what is built and what is not.

## The shape

```
UI  ──reads──▶  Room (source of truth)  ◀──writes──  SyncEngine
                      │                                   ▲
                      └──queued mutation──▶ PendingMutation┘
                                                           │
                                              GET /api/sync (delta)
                                              POST/PUT/DELETE (replay)
```

Room is the source of truth, not a cache. Every screen reads from it, so a
screen never shows a spinner waiting for the network and never shows stale data
after a write. The network only feeds sync.

## Why a counter and not `updated_at`

`sync_seq` is a monotonic per-row version, bumped by a SQLite trigger on every
insert and update. `updated_at` could not do the job for two reasons:

- `CURRENT_TIMESTAMP` has one-second resolution, so a bulk write produces ties
  that a cursor cannot advance past — the client would loop forever.
- Its format is `YYYY-MM-DD HH:MM:SS`, which does not compare correctly against
  the ISO-8601 strings a client sends. `'T'` (0x54) sorts after `' '` (0x20), so
  every row would look newer than the cursor.

Triggers rather than per-handler code: there are ~20 write paths across the seven
mirrored tables, and one missed path silently stops that table syncing with no
error anywhere. A trigger cannot be missed and is atomic with the write.

## Deletions

A hard delete leaves no row behind, so a delta cannot see it. Every delete path
writes a tombstone, addressed to **every user who could see the row** — not just
its owner. A tombstone addressed only to the owner leaves the row on every family
member's phone forever, because the delta stops returning it and nothing else
ever mentions it.

Tombstones are written by the handlers, not by triggers, because the fan-out
needs `account_members` and `family_members`, and for an account delete those
rows cascade away before the transaction trigger would run.

Cascade cases that need explicit tombstones:

- deleting an account removes its transactions (`ON DELETE CASCADE`)
- deleting one half of a transfer removes both rows
- deleting a category nulls `transactions.category_id` (an update, so it flows
  through the delta normally)

## Sharing

`sync_seq` cannot express "rows you could not see before". A row shared with you
today still carries an old version, so a delta would skip it and the account
would appear empty.

`user_visibility_version` is bumped whenever sharing changes — a member added or
removed, an account shared or unshared, a family joined or left. The client
compares the version in the sync response with the one it holds and, when they
differ, wipes its mirror and resyncs from zero.

## Conflicts

A queued update or delete carries `X-Base-Sync-Seq`: the version the edit was
based on. The server compares it with the row's current version and answers
`409 { error, current }` when they differ.

That is a lost update, not a duplicate row. The id is unique the whole time;
what collides is the version the edit was based on. Without the check, a phone
that was offline for a week would silently overwrite a family member's newer
change.

What is **not** a conflict:

- `404` on PUT/DELETE — the row is already gone. Mark synced, remove locally.
- creates — a new row has no server version to disagree with.
- validation `4xx` — parked, but as a failure rather than a version conflict.

A conflict is parked with both payloads and surfaced three ways: a banner, a
danger badge on the affected row, and a count on the Settings row. "Keep mine"
re-queues with the server's **current** version as the new base; re-queueing with
the old base would conflict again immediately, forever.

## Idempotency

A queued write that times out is retried. Without a key the retry inserts a
second transaction and moves the balance twice.

The key is generated once, when the mutation is queued, and reused on every
attempt. Only `2xx` responses are stored — caching a `409` would make every retry
return it forever. A replay re-reads the row's current `sync_seq` before
answering, because handing the client the version from the original write would
manufacture a false conflict on its next valid edit.

## Failure policy

| Response | Meaning | Action |
|---|---|---|
| 2xx | landed | clear mutation, adopt server row |
| 409 | someone changed it first | park a conflict |
| 404 on PUT/DELETE | already gone | drop locally, not a conflict |
| 401 | session died | keep the queue, ask the user to sign in |
| 403 CSRF | token expired (24h vs 7d session) | refresh, retry once |
| other 4xx | server refused it | park so the user can see why |
| 5xx / network | transient | keep the mutation, retry later |

A `401` never clears the queue. The JWT lives 7 days; a phone offline for a week
would otherwise lose every write it made.

## What is built

**Backend (`efina`)**

- `migrations/0084_offline_sync.sql` — `sync_sequence`, `sync_seq` on all seven
  tables with backfill and triggers, `sync_tombstones`, `idempotency_keys`,
  `user_visibility_version`, `categories.updated_at`
- `functions/utils/sync.js` — counter, tombstones with viewer fan-out, visibility
  bumps, idempotency wrapper, conflict helpers
- `functions/api/sync/index.js` — the delta endpoint
- tombstones wired into every delete path (accounts, categories, transactions,
  transfers, budgets, goals, debts, scheduled)
- visibility bumps on member add/remove, share/unshare, family join/leave
- idempotency + `X-Base-Sync-Seq` on transactions and accounts
- tests: `functions/api/sync/migration.test.js` (10), `sync.test.js` (13), both
  against real SQLite

**Android (`efina-android`)**

- Room schema with SQLCipher, denormalized list fields, `pendingState`
- `SyncEngine`, `SyncWorker` (Hilt, unique work), `MutationQueue`,
  `ConnectivityObserver`
- `OfflineBanner`, `ReauthBanner`, `SyncChip`, `PendingBadge`, `SyncStatusViewModel`
- shell wiring in `EfinaAppRoot`

## Repositories

All seven mirrored entities are Room-backed (`OfflineRepositories.kt`,
`OfflineRepositories2.kt`) and bound in `RepositoryModule`:

- reads observe the mirror (`observeAccounts()`, `observeTransactions()`, …)
- writes land in the mirror first, then queue a mutation
- `getX()` still exists and now *refreshes* the mirror; a failure is silent when
  the mirror already has data, so going offline does not blank a screen
- a row created offline gets a negative id; deleting it before it syncs simply
  drops it, because queueing a DELETE for an id the server has never seen would
  404 forever

Sharing (`setShared`, `shareWith`, `removeMember`, `setBudgetSharing`) stays
online-only: it changes who can see the data, which is exactly the thing that
must not be guessed offline.

Compound operations are queued as **one** mutation each, because the server
already applies them atomically in a `DB.batch`: `createTransfer`,
`addFunds`, `payDebt`.

## What is not built yet

Only one thing, and it is not code:

- **Nothing has been run on a device.** Everything here is proven by the
  compiler, by 5 Android test classes and by 72 backend tests against real
  SQLite, plus an end-to-end run against the deployed backend (one
  Idempotency-Key, two POSTs, one row, for six entities). No one has installed
  the APK and gone offline. Treat the first device run as the real acceptance
  test, particularly for SQLCipher key retrieval in the background worker and
  for the first-launch full sync.

Deliberately out of scope:

- **The web app does not consume `/api/sync`.** It keeps using the per-resource
  endpoints and its own localStorage cache. The backend changes are additive so
  it is unaffected.

## Test coverage

Android (`./gradlew testDebugUnitTest`, wired into CI):

| Class | Covers |
|---|---|
| `SyncClassifierTest` | the whole failure policy, including the easy-to-get-wrong cases: 404 on a create is a conflict not "already gone"; a second 403 after a CSRF refresh is a refusal, not a retry |
| `MirrorRulesTest` | a real in-memory Room DB: a delta pull never clobbers a pending edit, applies when synced, inserts unknown rows; a visibility wipe clears the mirror without touching the queue; queue count/delete/server-id binding |
| `ReplayRequestTest` | MockWebServer: the `Idempotency-Key` and `X-Base-Sync-Seq` headers on the wire, CSRF/session headers, a bodyless DELETE |
| `SyncWorkerEnqueueTest` | work-testing: unique work with KEEP so two triggers cannot race the queue; requires a network |

Backend (`npx vitest run functions/`): 72 tests over the migration and its
triggers, the delta endpoint, idempotency/replay, the tombstone fan-out and the
visibility version. The 15 failures on `master` are pre-existing — 12 are
miniflare failing to start in this environment, 3 are known gaps in the
transaction mock — and are unchanged by this work.
