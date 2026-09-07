# API Contract: Transfers

Base path: `/api/transfers`. Requires `Authorization: Bearer <supabase-jwt>`.

## `POST /api/transfers`

Executes a P2P transfer from the caller's wallet to another account (FR-006/FR-007/FR-008).

**Request**

```json
{
  "idempotencyKey": "8f14e45f-...",
  "recipientAccountId": "c4b2f7c1-...",
  "amount": "50.0000"
}
```

| Field | Required | Notes |
|---|---|---|
| `idempotencyKey` | yes | Client-generated UUID; a repeated key returns the original result (see research.md §3) instead of re-executing |
| `recipientAccountId` | yes | Must not equal the caller's own account id |
| `amount` | yes | Positive, ≤ 4 decimal places |

**Response 201**

```json
{
  "transactionId": "0f2c1a3b-...",
  "senderAccountId": "b3a1e6b0-...",
  "recipientAccountId": "c4b2f7c1-...",
  "amount": "50.0000",
  "status": "SUCCESS",
  "createdAt": "2026-09-07T12:05:00Z"
}
```

**Response 200** (duplicate submission, same idempotency key as a prior completed transfer):
returns the original transfer result unchanged, `error` is not set.

**Errors**:
- `VALIDATION_ERROR` (400) — non-positive amount, unsupported precision, missing fields.
- `SELF_TRANSFER_NOT_ALLOWED` (422) — recipient equals sender.
- `ACCOUNT_NOT_FOUND` (404) — recipient identifier unknown/malformed/inaccessible.
- `INSUFFICIENT_FUNDS` (422) — sender balance less than amount; neither balance changes.
- `UNAUTHENTICATED` (401).

**Concurrency contract**: Implementation acquires pessimistic write locks on both accounts in
ascending UUID order and performs debit + credit + audit-record insert as one atomic transaction
(constitution principle II); concurrent transfers against the same wallets never deadlock, never
produce a negative balance, and each accepted transfer is reflected exactly once (SC-002).
