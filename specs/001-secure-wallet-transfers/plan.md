# Implementation Plan: Secure Wallet and Peer-to-Peer Transfers

**Branch**: `001-secure-wallet-transfers` | **Date**: 2026-09-07 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-secure-wallet-transfers/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Deliver a secure, concurrency-safe wallet and P2P transfer platform: authenticated users create a
wallet, deposit funds, transfer to other accounts by identifier, and review paginated/filterable
transaction history through a dashboard with stable loading/success/empty/error states. The backend
is a Spring Boot 4.1.1 (Java 25, virtual threads) modular-feature-slice REST API backed by
PostgreSQL/Supabase, using `BigDecimal`/`NUMERIC(19,4)` money, pessimistic row locking with
deterministic lock ordering for atomic transfers, and stateless Supabase JWT (JWKS) verification. The
frontend is a Next.js 16 (App Router, Turbopack, React 19) app using an edge session proxy for
Supabase-backed auth, Server Actions + `useActionState` for transfer mutations, TanStack Query v5 for
balance/history fetching, and `nuqs` for URL-bound pagination/filter state, built with shadcn/ui.
Both are containerized via a multi-stage Docker build and orchestrated locally with Docker Compose.

## Technical Context

**Language/Version**: Backend: Java 25 (LTS). Frontend: TypeScript 5, Node.js runtime for Next.js 16.

**Primary Dependencies**: Backend: Spring Boot 4.1.1 (Web MVC, Security OAuth2 Resource Server, Data
JPA/Hibernate, Validation), springdoc-openapi-starter-webmvc-ui (Scalar `/docs`). Frontend: Next.js
16 (App Router, Turbopack), React 19, TanStack Query v5, nuqs, @supabase/ssr, @supabase/supabase-js,
Tailwind CSS, shadcn/ui, lucide-react.

**Storage**: PostgreSQL (Supabase-hosted), accessed via Spring Data JPA/Hibernate; Flyway-style SQL
migration (`V1__init_schema.sql`) defines `accounts` and `transactions` tables with `NUMERIC(19,4)`
money columns and a non-negative balance check constraint.

**Testing**: Backend: JUnit 5 + Spring Boot Test (`spring-boot-starter-*-test` modules) for
controller/service/repository slices and concurrency/integration tests. Frontend: not yet configured
in `package.json`; component/integration tests to be added (recommend Vitest/Playwright — see
research.md).

**Target Platform**: Backend: Linux containers (Docker, `eclipse-temurin:25-jre-alpine`) behind
Spring Boot embedded Tomcat with virtual threads. Frontend: Node.js/Edge runtime on Vercel-style or
containerized Next.js hosting.

**Project Type**: Web application (monorepo: Spring Boot API at repository root + Next.js app in
`/frontend`).

**Performance Goals**: 99.9% of valid deposit/transfer attempts produce a final outcome within 5s
(SC-001). Support 100 simultaneous money-movement requests against shared wallets with no lost
updates, no negative balances, and no deadlocks (SC-002). History search returns filtered results
within 3s for accounts with 10,000 transactions (SC-005).

**Constraints**: Monetary values MUST use `BigDecimal`/`NUMERIC(19,4)` — no binary floating point.
Transfers MUST be one atomic transaction with deterministic (sorted-UUID) pessimistic locking across
both accounts. All protected endpoints MUST reject missing/expired/malformed/invalid JWTs via JWKS
verification with no per-request DB session lookups. Every failure path MUST return the standardized
error payload (`status`, `error`, `message`, `timestamp`, `traceId`) without leaking implementation
details. `traceId` is an opaque request-correlation value that can be provided to support.
Pagination/filter state MUST be represented in the URL (`nuqs`). UI loading/empty/error states MUST
reserve final layout space (no shift).

**Scale/Scope**: 4 user stories (wallet creation/funding, P2P transfer, transaction history,
dashboard UX), single configured currency, history up to 10,000+ records per account with
pagination (default page size 25, max 100).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Self-Contained Domain Architecture** — PASS. Backend is organized as feature slices
  (`features/accounts`, `features/transfers`, `features/transactions`) each owning its entity,
  repository, service, controller, and DTOs; frontend mirrors this with `features/accounts`,
  `features/transfers`. Cross-feature access goes through service/repository methods, not shared
  mutable state.
- **II. Financial Precision and Transaction Safety** — PASS. `Account.balance` and
  `Transaction.amount` are `BigDecimal` mapped to `NUMERIC(19,4)`; `TransferService.executeTransfer`
  runs inside `@Transactional`, acquires `PESSIMISTIC_WRITE` locks in sorted-UUID order via
  `findByIdForUpdate`, and verifies sufficient balance before mutation.
- **III. Predictable User Experience** — PASS (design obligation carried into Phase 1). Centralized
  `@RestControllerAdvice` will standardize error payloads; `nuqs` binds transaction history
  page/filter state to the URL; dashboard components must reserve layout space for loading/empty/
  error/success states.
- **IV. Boundary Security and Efficient Authorization** — PASS. Spring Security OAuth2 Resource
  Server validates Supabase-issued JWTs against the JWKS endpoint (stateless, no DB lookup per
  request); the Next.js edge proxy (`proxy.ts`) enforces session presence before protected routes
  render and forwards the Bearer token downstream.

No violations requiring Complexity Tracking justification.

## Post-Design Constitution Re-Check

*Performed after Phase 1 design artifacts (data-model.md, contracts/, quickstart.md).*

- **I**: Data model and contracts keep `Account`, `Transaction` (Deposit/Transfer as `type`), and
  history query filters scoped to their owning feature packages; no new cross-domain coupling
  introduced. PASS.
- **II**: `data-model.md` confirms `NUMERIC(19,4)` for all money fields and documents the sorted-UUID
  locking order and atomic transaction boundary for both deposit and transfer paths. PASS.
- **III**: `contracts/` define one standardized error schema reused across all endpoints and encode
  pagination/filter query parameters explicitly (`page`, `size`, `type`, `status`, `from`, `to`,
  `counterpartyId`) so the frontend can bind them 1:1 via `nuqs`. PASS.
- **IV**: `contracts/` mark every wallet/transfer/history endpoint as requiring a Bearer JWT and
  document the 401/403 standardized error responses. PASS.

No new violations; Complexity Tracking remains empty.

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
# Monorepo web application: Spring Boot API in /backend, Next.js app in /frontend
backend/src/main/java/com/wallet/api/
├── WalletApiApplication.java
├── config/
│   ├── SecurityConfig.java              # Supabase JWT resource-server config (JWKS)
│   ├── GlobalExceptionHandler.java      # @RestControllerAdvice standardized error payload
│   └── ScalarApiDocsConfig.java         # springdoc-openapi + Scalar UI at /docs
└── features/
    ├── accounts/
    │   ├── Account.java, AccountRepository.java, AccountService.java, AccountController.java
    │   └── dto/AccountResponse.java
    ├── transfers/
    │   ├── TransferController.java, TransferService.java
    │   └── dto/TransferRequest.java
    └── transactions/
        ├── Transaction.java, TransactionRepository.java, TransactionController.java
        └── dto/ (history query/response records — Phase 1)

backend/src/main/resources/
├── application.yml                      # spring.threads.virtual.enabled, JPA, JWKS issuer/jwk-set-uri
└── db/migration/V1__init_schema.sql     # accounts, transactions tables (NUMERIC(19,4))

backend/src/test/java/com/wallet/api/    # unit + integration + concurrency tests per feature

frontend/
├── src/
│   ├── proxy.ts                         # Edge session proxy (@supabase/ssr, Bearer forwarding)
│   ├── app/                             # App Router pages (dashboard, login, signup)
│   ├── components/ui/                   # shadcn/ui primitives (button, card, table, dialog, toaster)
│   ├── features/
│   │   ├── accounts/components/BalanceCard.tsx
│   │   └── transfers/
│   │       ├── actions/transfer.ts       # Server Action bound to useActionState
│   │       └── components/TransferForm.tsx
│   └── lib/
│       ├── api-client.ts                 # apiFetch wrapper used by TanStack Query
│       ├── react-query.ts                # QueryClient setup
│       └── supabase/{client,server}.ts
└── (tests to be added — see research.md)

backend/Dockerfile                         # multi-stage: eclipse-temurin:25-jdk-alpine build → 25-jre-alpine runtime
docker-compose.yml                         # local Postgres/PgAdmin (new, Phase 1 output referenced in quickstart.md)
```

**Structure Decision**: Web application monorepo — Option 2 pattern, with the Spring Boot backend
under `/backend` and the Next.js frontend under `/frontend`. Shared infrastructure remains at the
repository root.

## Complexity Tracking

*No constitution violations identified; this section is intentionally empty.*
