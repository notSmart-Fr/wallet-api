# Quickstart: Secure Wallet and Peer-to-Peer Transfers

Validation guide for the feature described in [spec.md](./spec.md), designed per [plan.md](./plan.md),
[data-model.md](./data-model.md), and [contracts/](./contracts/).

## Prerequisites

- Java 25, Maven wrapper (`./mvnw`), Docker/Docker Compose.
- A Supabase project with Auth enabled (project ref, JWKS endpoint reachable) and a Postgres
  database (local Docker Compose Postgres is sufficient for backend-only validation).
- Node.js (per `frontend/package.json` engines) and a package manager for the `frontend/` workspace.
- Two Supabase-authenticated test users (or two valid JWTs) to exercise transfers between accounts.

## 1. Start local infrastructure

```powershell
docker compose up -d
```

Brings up local PostgreSQL (and PgAdmin) per `docker-compose.yml` (Phase 1 deliverable —
see research.md §9). Confirm `wallet_db` is reachable at `localhost:5432` matching
`application.yml`'s `spring.datasource.url`.

## 2. Run the backend

```powershell
$env:SUPABASE_PROJECT_REF = "<your-supabase-project-ref>"
./mvnw spring-boot:run
```

Confirm startup logs show virtual threads enabled and the JWKS issuer/jwk-set-uri resolved.

## 3. Validate API docs

Open `http://localhost:8080/docs` and confirm the embedded Scalar UI renders the OpenAPI 3 spec
covering the accounts, transfers, and transactions endpoints in `contracts/`.

## 4. Validate User Story 1 — Create and Fund a Wallet

1. Call `GET /api/accounts/me` with User A's Bearer token → expect 201/200 with a zero-balance
   account and a unique `id` (contracts/accounts-api.md).
2. Call `POST /api/accounts/deposits` with `{"amount": "100.0000"}` → expect 201, `newBalance`
   increased by exactly 100.0000, and a new `SUCCESS`/`DEPOSIT` transaction row.
3. Retry with `{"amount": "0"}` and `{"amount": "-5"}` → expect `VALIDATION_ERROR` (400), a non-empty
   `traceId`, and balance unchanged (spec Acceptance Scenario US1.4).

## 5. Validate User Story 2 — Transfer Funds to Another Account

1. Fund User A (as above) and provision User B's account.
2. Call `POST /api/transfers` as User A with `recipientAccountId` = User B's account id and a fresh
   `idempotencyKey` → expect 201, sender debited, recipient credited by the same amount, one
   `SUCCESS`/`TRANSFER` transaction row.
3. Repeat the exact same request body (same `idempotencyKey`) → expect the original result returned
   without a second balance change (research.md §3, FR-017).
4. Attempt a transfer exceeding User A's balance → expect `INSUFFICIENT_FUNDS` (422), no balance
   change.
5. Attempt a transfer where `recipientAccountId` equals User A's own account id →
   `SELF_TRANSFER_NOT_ALLOWED` (422).
6. Attempt a transfer to an unknown UUID → `ACCOUNT_NOT_FOUND` (404).
7. (Concurrency) Fire ~100 simultaneous transfer requests touching a shared pair of wallets (e.g.,
   via a small load-test script) → expect no negative balances, no deadlock/timeout errors, and
   exactly one transaction row per accepted transfer (SC-002).

## 6. Validate User Story 3 — Review Transaction History

1. Call `GET /api/transactions?page=0&size=25` as User A → expect reverse-chronological results
   containing the deposit and transfer created above.
2. Call with `type=TRANSFER`, `status=SUCCESS`, a `from`/`to` range, and `counterpartyId` set to
   User B's id → expect every returned record to match all active filters.
3. Call `GET /api/transactions` using User B's token with `counterpartyId` unset → confirm User B
   cannot see User A-only records outside their shared transfer, and that attempting to read
   another account's history directly is denied without detail leakage (SC-007).
4. Request a page far beyond available data → expect an empty `content` array, not an error.

## 7. Validate User Story 4 — Dashboard UX (frontend)

```powershell
cd frontend
npm install
npm run dev
```

1. Sign in as User A; open the dashboard while throttling the network (e.g., DevTools "Slow 3G") →
   confirm balance/history areas show skeleton placeholders matching final layout dimensions (no
   shift).
2. Submit a transfer via `TransferForm.tsx`; confirm the submit control disables and shows a
   pending state (via `useActionState`) and cannot be double-submitted.
3. Confirm a successful transfer updates the dashboard balance and shows a success message; force a
   backend error (e.g., stop the backend) and confirm a standardized, user-safe error message
   appears without leaking implementation details.
4. Confirm transaction table pagination controls update the `page`/`size` URL query parameters
   (via `nuqs`) and that reloading the URL restores the same page/filter view.
5. For a brand-new account with no transactions, confirm the history view shows a distinct empty
   state (not a loading spinner or error).

## 8. Security checks

- Call any protected endpoint with no `Authorization` header, an expired token, and a malformed
   token → expect `UNAUTHENTICATED` (401) in all three cases, with the standardized error payload and
   a non-empty `traceId` that also appears in the `X-Trace-Id` response header.
- Confirm `/api/transactions`, `/api/accounts/me`, and `/api/transfers` never return another
  account's data regardless of query parameters supplied.

## Done When

- All steps above produce the documented expected outcomes with no manual workarounds.
- Docker Compose, backend, and frontend all start cleanly from a fresh checkout using only the
  commands in this guide.
