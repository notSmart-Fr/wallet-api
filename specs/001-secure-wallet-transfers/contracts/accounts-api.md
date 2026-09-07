# API Contract: Wallet & Accounts

Base path: `/api/accounts`. All endpoints require `Authorization: Bearer <supabase-jwt>`.

## `GET /api/accounts/me`

Returns the authenticated caller's own wallet.

**Response 200**

```json
{
  "id": "b3a1e6b0-6e2a-4b7e-9b2f-3c1d2e4f5a6b",
  "balance": "1250.5000",
  "createdAt": "2026-09-01T10:15:00Z"
}
```

**Errors**: `UNAUTHENTICATED` (401) if the token is missing/invalid/expired.

## `POST /api/accounts` (implicit on first authenticated access, or explicit provisioning)

Creates the caller's wallet if one does not already exist (FR-001). Idempotent per `userId`
(unique constraint) — calling again for an already-provisioned user returns the existing account
rather than erroring.

**Response 201** (created) or **200** (already existed) — same `AccountResponse` shape as above.

**Errors**: `UNAUTHENTICATED` (401).

## `POST /api/accounts/deposits`

Deposits funds into the caller's own wallet (FR-004/FR-005).

**Request**

```json
{ "amount": "100.0000" }
```

**Response 201**

```json
{
  "transactionId": "0f2c1a3b-...",
  "accountId": "b3a1e6b0-...",
  "newBalance": "1350.5000",
  "status": "SUCCESS",
  "createdAt": "2026-09-07T12:00:00Z"
}
```

**Errors**: `VALIDATION_ERROR` (400) — non-positive amount or unsupported precision, balance
unchanged. `UNAUTHENTICATED` (401).
