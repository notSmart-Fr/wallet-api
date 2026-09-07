# Phase 0 Research: Secure Wallet and Peer-to-Peer Transfers

## 1. Money precision & schema representation

- **Decision**: Represent all monetary fields as Java `BigDecimal` mapped to PostgreSQL
  `NUMERIC(19,4)`. Reject any input amount whose scale exceeds 4 decimal places or that isn't
  strictly positive, before any balance mutation.
- **Rationale**: `NUMERIC(19,4)` supports up to 15 integer digits and 4 fractional digits, enough
  for realistic wallet balances while avoiding binary floating-point rounding error. This matches
  the constitution's "Financial Precision and Transaction Safety" principle and the existing
  `V1__init_schema.sql` / `Account.java` / `Transaction.java` implementation.
- **Alternatives considered**: `double`/`float` (rejected — binary rounding error, constitution
  prohibits it); integer minor-units (e.g., cents) (rejected — less direct fit with Supabase/Postgres
  numeric tooling and would require a migration of the already-created schema).

## 2. Concurrency control for transfers

- **Decision**: Use `@Lock(LockModeType.PESSIMISTIC_WRITE)` row locks acquired in ascending UUID
  order (`findByIdForUpdate`), inside one `@Transactional` method (`TransferService.executeTransfer`)
  that debits sender, credits recipient, and inserts one `Transaction` row atomically.
  Balance-sufficiency is checked only after both locks are held.
- **Rationale**: Sorted-UUID lock acquisition is a standard deadlock-avoidance technique for
  multi-row updates: as long as every transaction acquires locks in the same total order, circular
  wait (and thus deadlock) cannot occur. This satisfies FR-008/FR-010 and SC-002 (100 concurrent
  transfer requests, no deadlock, no negative balance, exactly-once accepted transfers).
- **Alternatives considered**: Optimistic locking with `@Version` + retry loop (rejected as primary
  mechanism — under high contention on popular wallets it produces many retries/aborts and does not
  as directly guarantee bounded latency for SC-001; may be layered later for read-heavy paths but
  is not required for this feature). Application-level distributed locks (rejected — unnecessary
  operational complexity for a single-database deployment).

## 3. Idempotent transfer submission / retry safety

- **Decision**: Require clients to submit an idempotency key (a client-generated UUID) with each
  transfer request; the backend persists a unique constraint on (idempotency key) scoped to the
  transfer feature and returns the original result for a repeated key instead of re-executing the
  transfer. The `useActionState`-bound Server Action generates and reuses this key across a retry
  of the same in-flight submission.
- **Rationale**: Directly satisfies FR-017 (retry after an uncertain response must not duplicate
  the transfer) and the edge case "retried after a timeout must not create duplicate money
  movement". A unique DB constraint makes duplicate suppression atomic and race-free, independent of
  which app instance handles the retry (works under virtual threads / multiple pods).
- **Alternatives considered**: Client-only disable-button debouncing (rejected as sole mechanism —
  does not protect against network-level retries or duplicate tabs); distributed cache-based
  dedupe (rejected — adds an extra moving part when a DB unique constraint already gives
  transactional consistency).

## 4. Authentication & authorization (Supabase JWT)

- **Decision**: Spring Security OAuth2 Resource Server validates Supabase-issued JWTs using the
  JWKS endpoint (`issuer-uri` / `jwk-set-uri` already configured in `application.yml`), so signature
  verification is stateless and requires no per-request database call. The Next.js edge proxy
  (`proxy.ts`) uses `@supabase/ssr` to check session validity before rendering `/dashboard` routes
  and forwards the access token as a `Bearer` header to the backend.
- **Rationale**: Matches constitution principle IV (stateless, constant-time boundary
  authorization) and FR-002/FR-014. JWKS caching (built into Spring Security's
  `NimbusJwtDecoder`) avoids repeated network calls per request.
- **Alternatives considered**: Backend session cookies + server-side session store (rejected —
  requires a DB/cache lookup per request, violates "avoid unnecessary session lookups").

## 5. Standardized error payload

- **Decision**: A single `@RestControllerAdvice` (`GlobalExceptionHandler`) maps every domain
  exception (validation, not-found, insufficient-funds, self-transfer, unauthorized, duplicate
  submission, unexpected) to one JSON shape: `{ status, error, message, timestamp, traceId }`, with
  a fixed set of `error` codes documented in `contracts/errors.md`. A request-bound opaque `traceId`
  is created at the API boundary, returned with every error, and recorded with related diagnostic logs.
- **Rationale**: Satisfies FR-015 and constitution principle III; keeps the frontend's error
  handling generic (one shape to parse) while still allowing distinct user-facing messages per
  `error` code.
- **Alternatives considered**: Per-endpoint bespoke error shapes (rejected — harder to test as a
  contract, inconsistent frontend handling).

## 6. Transaction history pagination & URL-bound filters

- **Decision**: `GET /api/transactions` accepts `page`, `size` (default 25, max 100), `type`,
  `status`, `from`, `to`, `counterpartyId` query parameters, returns a page envelope
  (`content`, `page`, `size`, `totalElements`, `totalPages`). The frontend binds these same
  parameter names via `nuqs` so the URL is the single source of truth for view state (shareable,
  restorable, back/forward-navigable).
- **Rationale**: Directly satisfies FR-012/FR-013 and SC-005; using Spring Data `Pageable` plus the
  existing `idx_tx_sender`/`idx_tx_recipient` indexes (sender/recipient + `created_at DESC`) keeps
  filtered/paginated queries fast at 10,000+ rows.
- **Alternatives considered**: Cursor-based pagination (rejected for v1 — offset-based `Pageable`
  is simpler to bind to `nuqs` URL state and sufficient at the stated scale; can be revisited if
  history sizes grow far beyond 10k).

## 7. Frontend data-fetching & mutation strategy

- **Decision**: TanStack Query v5 (via a shared `QueryClient` in `lib/react-query.ts` and a fetch
  wrapper in `lib/api-client.ts`) handles all read paths (balance, history) with background
  revalidation and skeleton fallbacks. React 19 Server Actions (`features/transfers/actions/
  transfer.ts`) handle the transfer mutation, bound to `useActionState` in `TransferForm.tsx` to
  get built-in pending/error state without extra client-side state management.
- **Rationale**: Matches the required stack and constitution principle III (loading/error/empty
  states with stable layout); Server Actions integrate naturally with Next.js 16's App Router and
  avoid hand-rolled fetch/loading state for the one write path that matters most (transfers).
  After a successful action, the relevant TanStack Query keys (balance, history) are invalidated to
  refresh the dashboard.
- **Alternatives considered**: Pure client-side `fetch` + local `useState` for mutation state
  (rejected — duplicates what `useActionState` + Server Actions already provide, and is explicitly
  requested against in the tech stack).

## 8. API documentation

- **Decision**: `springdoc-openapi-starter-webmvc-ui` (already in `pom.xml`) generates the OpenAPI
  3 spec; `ScalarApiDocsConfig` exposes an embedded Scalar UI at `/docs` pointing at the generated
  spec, replacing/complementing the default Swagger UI.
- **Rationale**: Matches the required tech stack; gives the frontend and any external consumers a
  live, versioned contract view without hand-maintained docs drifting from the code.
- **Alternatives considered**: Hand-written OpenAPI YAML (rejected — drifts from implementation);
  default Swagger UI only (acceptable fallback, but Scalar UI is the specified requirement).

## 9. Local orchestration & deployment

- **Decision**: Add a `docker-compose.yml` at the repo root defining `postgres` (with a named
  volume) and `pgadmin` services for local development; rely on Spring Boot's Docker Compose
  integration (`spring-boot-docker-compose`) for automatic local discovery when running the app
  outside its own container. The existing multi-stage `Dockerfile`
  (`eclipse-temurin:25-jdk-alpine` build → `eclipse-temurin:25-jre-alpine` runtime, non-root
  `spring` user) is reused unchanged for the production image.
- **Rationale**: Matches the required tech stack section 4; keeps local development
  (`docker compose up`) and containerized production builds cleanly separated.
- **Alternatives considered**: Testcontainers-only local dev (rejected as the sole mechanism —
  useful for automated tests, but `docker-compose.yml` is explicitly requested for interactive
  local orchestration with PgAdmin).

## 10. Frontend/backend test tooling (gap identified)

- **Decision**: Backend testing uses the already-present `spring-boot-starter-*-test` dependencies
  (JUnit 5, Spring Security Test, Data JPA Test) for unit, slice, and concurrency/integration tests.
  Frontend has no test runner configured yet; recommend adding Vitest + React Testing Library for
  component/unit tests and Playwright for end-to-end dashboard/transfer flow tests, introduced as
  implementation tasks rather than blocking this plan.
- **Rationale**: Keeps this plan unblocked while flagging a concrete, actionable follow-up; matches
  common Next.js 16/React 19 testing conventions.
- **Alternatives considered**: Jest (rejected — Vitest has faster ESM/Turbopack-aligned defaults
  for a Next.js 16 + TypeScript project).

All "NEEDS CLARIFICATION" items from the Technical Context are resolved above; none remain open.
