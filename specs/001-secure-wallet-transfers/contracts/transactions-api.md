# API Contract: Transaction History

Base path: `/api/transactions`. Requires `Authorization: Bearer <supabase-jwt>`.

## `GET /api/transactions`

Returns a reverse-chronological, paginated, filtered view of the caller's own transaction history
(FR-012/FR-013). Always scoped to the authenticated caller's `Account`; no combination of query
parameters can return another account's records (FR-014, SC-007 → results in `FORBIDDEN` cannot
even be triggered because scoping is implicit, not by a caller-supplied account id).

**Query parameters** (each MUST be mirrored 1:1 in the frontend URL via `nuqs`):

| Param | Type | Default | Notes |
|---|---|---|---|
| `page` | int | `0` | Zero-based |
| `size` | int | `25` | Max `100` |
| `type` | `DEPOSIT` \| `TRANSFER` | (none) | Optional filter |
| `status` | `SUCCESS` \| `FAILED` | (none) | Optional filter |
| `from` | ISO-8601 timestamp | (none) | Inclusive lower bound on `createdAt` |
| `to` | ISO-8601 timestamp | (none) | Inclusive upper bound on `createdAt` |
| `counterpartyId` | UUID | (none) | Restrict to transactions involving this other account |

**Response 200**

```json
{
  "content": [
    {
      "id": "0f2c1a3b-...",
      "type": "TRANSFER",
      "status": "SUCCESS",
      "amount": "50.0000",
      "senderAccountId": "b3a1e6b0-...",
      "recipientAccountId": "c4b2f7c1-...",
      "createdAt": "2026-09-07T12:05:00Z"
    }
  ],
  "page": 0,
  "size": 25,
  "totalElements": 137,
  "totalPages": 6
}
```

**Errors**: `VALIDATION_ERROR` (400) — `size` > 100, `from` after `to`, invalid enum value.
`UNAUTHENTICATED` (401).

A request for a page beyond the available data returns an empty `content` array with correct
`totalElements`/`totalPages` (edge case: "page requested beyond available history").
