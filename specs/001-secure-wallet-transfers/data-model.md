# Phase 1 Data Model: Secure Wallet and Peer-to-Peer Transfers

## Account

Represents a person's authenticated wallet identity (backed by `accounts` table).

| Field | Type | Constraints | Notes |
|---|---|---|---|
| `id` | UUID | PK, generated | Public account identifier used by other users to receive transfers |
| `userId` | UUID | NOT NULL, UNIQUE | Links to the Supabase-authenticated identity (JWT `sub` claim) |
| `balance` | `BigDecimal` / `NUMERIC(19,4)` | NOT NULL, DEFAULT 0.0000, `CHECK (balance >= 0)` | Never negative (FR-009); updated only inside atomic deposit/transfer transactions |
| `createdAt` | `OffsetDateTime` | NOT NULL, immutable | Set once at creation |

**Validation rules**:
- Exactly one `Account` per `userId` (unique constraint enforces FR-001's "one wallet account").
- `balance` scale MUST be 4 digits; reject writes that would violate the NUMERIC(19,4) precision or the non-negative check.

**Relationships**: One `Account` has many `Transaction` rows as sender (`senderAccount`, nullable — null for deposits) and many as recipient (`recipientAccount`, required).

## Transaction (audit record for both Deposit and Transfer)

Backed by `transactions` table; the `type` discriminates Deposit vs Transfer within one immutable audit entity (Key Entity "Transaction Record", plus Deposit/Transfer conceptual entities).

| Field | Type | Constraints | Notes |
|---|---|---|---|
| `id` | UUID | PK, generated | |
| `senderAccount` | Account (FK, nullable) | NULL for `DEPOSIT`, required for `TRANSFER` | Null represents "external funding source" for a deposit |
| `recipientAccount` | Account (FK) | NOT NULL | For a deposit, equals the depositing account; for a transfer, the receiving account |
| `amount` | `BigDecimal` / `NUMERIC(19,4)` | NOT NULL, `CHECK (amount > 0)` | Same precision/rounding policy as `Account.balance` |
| `type` | enum (`DEPOSIT`, `TRANSFER`) | NOT NULL | |
| `status` | enum (`SUCCESS`, `FAILED`) | NOT NULL | Rejected attempts MUST also be recorded per FR-011/FR-015 (see Notes) |
| `createdAt` | `OffsetDateTime` | NOT NULL, immutable | Used for reverse-chronological ordering and date-range filters |

**Notes / gap vs. current code**: FR-011 requires an immutable record for every *accepted or
rejected* deposit/transfer. The current `Transaction.TransactionStatus` enum only has `SUCCESS`/
`FAILED`; validation failures that occur before any row can be constructed (e.g., malformed
recipient, self-transfer) must still be captured — implementation tasks should extend recording so
a `FAILED` transaction row (or a distinct rejection log) is written for every rejected attempt
without violating the non-negative/atomicity constraints. This is an implementation-phase (tasks.md)
concern, not a data-model contract change.

**Validation rules**:
- `amount` scale ≤ 4 digits, strictly positive.
- `TRANSFER`: `senderAccount != recipientAccount` (no self-transfer, FR-007); both accounts must
  exist and be locked in ascending-UUID order before balance mutation.
- Immutable after creation: no update/delete path is exposed (Acceptance Scenario US3.4).

**Indexes** (already present): `idx_tx_sender (sender_account_id, created_at DESC)`,
`idx_tx_recipient (recipient_account_id, created_at DESC)` — support fast reverse-chronological,
paginated, per-account history lookups (SC-005).

## TransactionQuery (request-side view state, not persisted)

Represents the page/filter/order parameters used to retrieve one account holder's authorized
history (Key Entity "Transaction Query").

| Field | Type | Constraints | Notes |
|---|---|---|---|
| `page` | int | ≥ 0, default 0 | |
| `size` | int | 1–100, default 25 | Assumption: max page size 100 |
| `type` | enum (`DEPOSIT`, `TRANSFER`) optional | | |
| `status` | enum (`SUCCESS`, `FAILED`) optional | | |
| `from`, `to` | `OffsetDateTime` optional | `from <= to` when both present | Date-range filter |
| `counterpartyId` | UUID optional | | Filters to transactions involving a specific other account |

**Validation rules**: Always implicitly scoped to the authenticated caller's own `Account` (via
`userId` → `Account.id`); no combination of filters can surface another account's records
(FR-012/SC-007). Bound 1:1 to `nuqs`-managed URL query parameters on the frontend.

## State Transitions

- **Account.balance**: mutated only within `DepositService`/`TransferService` transactional
  methods; every mutation is paired with exactly one new `Transaction` row in the same DB
  transaction (all-or-nothing, FR-008).
- **Transaction**: write-once (`SUCCESS` or `FAILED`); no further transitions — matches the
  "immutable audit entry" requirement.

## Idempotency (supports research.md §3)

An idempotency key column (client-generated UUID) with a unique constraint should be added to the
transfer write path in the implementation phase so repeated submissions of the same logical
transfer return the original result instead of re-executing it (FR-017). Modeled here as a
constraint addition, not a new entity.
