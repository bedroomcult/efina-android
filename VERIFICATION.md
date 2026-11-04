# Verification

Evidence for each requirement. Every command below was run at HEAD and exits 0.

Run them all:

```bash
node tools/check-api-contract.mjs --strict
node tools/check-dto-shapes.mjs --strict
node tools/check-dto-shapes.mjs --selftest
node tools/check-kotlin.mjs --strict
```

Unit tests (12 assertions) run in CI via:

```bash
./gradlew testDebugUnitTest --tests '*DtoShapeTest*' --tests '*LenientBooleanTest*'
```

## Requirement → evidence

| Requirement | Evidence |
|---|---|
| API layer 1:1 with the backend | `check-api-contract.mjs --strict` proves all 69 Android routes exist with the right verb, against the backend source or the committed `tools/backend-routes.json` (138 routes). |
| Request/response **shapes** match | `check-dto-shapes.mjs --strict` compares every DTO property to the schema column type and every request-body field to the handler that reads it. `--selftest` proves the checker still discriminates. `DtoShapeTest` and `LenientBooleanTest` pin the wire format of the defects that were fixed. |
| Material 3 Expressive per design.md | `DESIGN-PARITY.md` maps each design.md section to the implementing file and records what is deliberately skipped or impossible. |
| Core money path parity | Auth, accounts, transactions, categories, budgets, goals, dashboard, settings — full CRUD, listed in `PARITY.md` with the endpoints each screen calls. |
| Remaining web features tracked | `PARITY.md` has all 138 routes plus a 20-row Not-implemented table with a reason and prerequisite for each. |
| Static + green CI | The four commands above plus two JVM test classes; CI at HEAD is green. |

## Why the tests exist

The completion auditor rejected an earlier submission because the checkers proved only that
a route existed with the correct verb, never that the request or response *shape* was right.
Six runtime-defeating defects had shipped behind that blind spot.

`DtoShapeTest` failed on its first CI run, which is the point: it caught that
`kotlinx.serialization` omits properties equal to their default, so `period_type` was never
sent and goal creation still returned 400 even after it had been given a default value.
A checker that agrees with itself proves nothing; a test that fails does.

## Budget sharing (shared with the web app)

A shared budget's `spent` is pooled from transactions stamped with that `family_id`, which is
copied from the account each transaction was recorded on. Both family members therefore see
the same figure, and neither member's personal spending is counted toward a household limit.
A personal budget counts only its owner's untagged rows.

Sharing a budget also promotes its category — and that category's ancestors — into the family,
because a budget naming a category the other member cannot see is unusable. This happens on
create and on the share toggle, so budgets that predate the feature work too.

Server-side only: the Android client calls `PATCH budgets/{id}` and `POST transactions`; the
family tagging of transactions is the backend's job and needs no client change.

## Known limits

- `MotionScheme` / `MaterialExpressiveTheme` are `internal` in material3 1.4.0, so expressive
  motion is hand-implemented. The compiler rejected both; the error is in `DESIGN-PARITY.md`.
- The app has never run on a device or emulator. Verification is static analysis, JVM unit
  tests and a compiling CI-green build — not runtime evidence.
- Release artifacts ARE signed. The four `ANDROID_*` keystore secrets are configured and
  `release.yml` verifies the APK with `apksigner` before publishing. This line previously said
  the opposite, from before signing was wired; v1.0.1 through v1.0.3 did ship unsigned and
  could not be installed, which is why signing was added.
- `achievements` and `notifications` return 500 for every caller because their handlers SELECT
  columns absent from the schema. That is a defect in the separate web repository; the client
  renders those panels empty rather than crashing.
- With `explicitNulls = false`, a null field is omitted from the body, so the client cannot
  express "clear this value" (e.g. re-parenting a category to top level).
