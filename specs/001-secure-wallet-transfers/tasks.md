# Tasks: Secure Wallet and Peer-to-Peer Transfers

**Input**: Design documents from `/specs/001-secure-wallet-transfers/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), and [contracts/](./contracts/)

**Tests**: Backend contract, integration, repository, and concurrency tests plus frontend component and end-to-end tests are included because this feature has public financial and error-response contracts.

**Organization**: Tasks are grouped by user story. Backend and frontend work for a story is included in its phase; contract and integration validation is owned by the same slice.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare repeatable local and automated validation for the monorepo.

- [X] T001 [P] Add Vitest, React Testing Library, Playwright, and test scripts in `frontend/package.json`.
- [X] T002 [P] Add Vitest and Playwright configuration files at `frontend/vitest.config.ts` and `frontend/playwright.config.ts`.
- [X] T003 [P] Add PostgreSQL Testcontainers test dependencies and test configuration in `pom.xml` and `src/test/resources/application-test.yml`.
- [X] T004 [P] Add local PostgreSQL and PgAdmin orchestration in `docker-compose.yml`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish JWT boundary behavior and one safe, traceable public error contract before exposing any feature endpoint.

**CRITICAL**: Complete this phase before beginning a user-story phase.

- [X] T005 Configure stateless Supabase JWT validation, authentication entry-point errors, and access-denied errors in `src/main/java/com/wallet/api/config/SecurityConfig.java`.
- [X] T006 Add request-scoped trace identifier generation and propagation in `src/main/java/com/wallet/api/config/TraceIdFilter.java`.
- [X] T007 Replace generic exception mapping with stable, user-safe error codes and mandatory `traceId` responses in `src/main/java/com/wallet/api/config/GlobalExceptionHandler.java`.
- [X] T008 [P] Add standardized error payload contract tests, including non-empty opaque `traceId`, in `src/test/java/com/wallet/api/config/GlobalExceptionHandlerTest.java`.
- [X] T009 [P] Add shared error parsing and trace-ID-safe client errors in `frontend/src/lib/api-client.ts`.
- [X] T010 Document the `traceId`-inclusive error contract in `specs/001-secure-wallet-transfers/plan.md`, `specs/001-secure-wallet-transfers/research.md`, and `specs/001-secure-wallet-transfers/quickstart.md`.

**Checkpoint**: Protected endpoints consistently reject invalid authentication and every public failure uses the documented error payload with `traceId`.

---

## Phase 3: User Story 1 - Create and Fund a Wallet (Priority: P1) - MVP

**Goal**: An authenticated user can receive one wallet, view its balance, and make a valid deposit with an immutable audit record.

**Independent Test**: Provision a user account, deposit `100.0000`, confirm the balance increases by that amount and a `SUCCESS` deposit appears in history; reject zero, negative, or over-precision amounts without balance changes.

- [ ] T011 [P] [US1] Add account provisioning, decimal-scale validation, and deposit-audit integration tests in `src/test/java/com/wallet/api/features/accounts/AccountControllerIntegrationTest.java`.
- [ ] T012 [P] [US1] Add `DEPOSIT` transaction persistence and account-scoped lookup support in `src/main/java/com/wallet/api/features/transactions/TransactionRepository.java`.
- [ ] T013 [US1] Create the completed deposit transaction in the same transaction as the balance update in `src/main/java/com/wallet/api/features/accounts/AccountService.java`.
- [ ] T014 [US1] Align wallet retrieval, implicit provisioning, deposit status codes, and contract errors in `src/main/java/com/wallet/api/features/accounts/AccountController.java`.
- [ ] T015 [US1] Align the wallet and deposit response DTOs with `contracts/accounts-api.md` in `src/main/java/com/wallet/api/features/accounts/dto/AccountResponse.java` and `src/main/java/com/wallet/api/features/accounts/dto/DepositResponse.java`.
- [ ] T016 [US1] Add account and deposit query functions with stable loading and user-safe error states in `frontend/src/features/accounts/api.ts` and `frontend/src/features/accounts/components/BalanceCard.tsx`.
- [ ] T017 [US1] Add BalanceCard loading, success, and error component tests in `frontend/src/features/accounts/components/__tests__/BalanceCard.test.tsx`.

**Checkpoint**: User Story 1 is independently functional and its deposit audit, money precision, and error-contract behavior pass integration tests.

---

## Phase 4: User Story 2 - Transfer Funds to Another Account (Priority: P1)

**Goal**: An authenticated account holder transfers funds once, safely retries an uncertain submission, and receives clear outcomes for all rejected transfers.

**Independent Test**: Fund two accounts, submit a transfer with an idempotency key, retry the identical request, and verify exactly one debit, credit, and audit record; then exercise insufficient-funds, self-transfer, and unknown-recipient failures.

- [ ] T018 [P] [US2] Add transfer success, rejection-code, duplicate-submission, and atomicity integration tests in `src/test/java/com/wallet/api/features/transfers/TransferControllerIntegrationTest.java`.
- [ ] T019 [P] [US2] Add concurrent-transfer locking and exact-balance integration tests using PostgreSQL Testcontainers in `src/test/java/com/wallet/api/features/transfers/ConcurrencyIntegrationTest.java`.
- [ ] T020 [US2] Add a unique transfer idempotency-key migration in `src/main/resources/db/migration/V2__add_transfer_idempotency_key.sql`.
- [ ] T021 [US2] Map the persisted idempotency key and transfer result fields in `src/main/java/com/wallet/api/features/transactions/Transaction.java` and `src/main/java/com/wallet/api/features/transactions/TransactionRepository.java`.
- [ ] T022 [US2] Add validated idempotency-key input and the documented transfer result DTO in `src/main/java/com/wallet/api/features/transfers/dto/TransferRequest.java` and `src/main/java/com/wallet/api/features/transfers/dto/TransferResponse.java`.
- [ ] T023 [US2] Implement duplicate-result lookup, typed rejection exceptions, sorted pessimistic locks, and atomic transfer auditing in `src/main/java/com/wallet/api/features/transfers/TransferService.java`.
- [ ] T024 [US2] Map transfer responses and stable error codes to the public contract in `src/main/java/com/wallet/api/features/transfers/TransferController.java` and `src/main/java/com/wallet/api/config/GlobalExceptionHandler.java`.
- [ ] T025 [US2] Generate and reuse an idempotency key for an in-flight transfer and invalidate balance/history queries after success in `frontend/src/features/transfers/actions/transfer.ts`.
- [ ] T026 [US2] Complete pending, success, and user-safe error behavior in `frontend/src/features/transfers/components/TransferForm.tsx`.
- [ ] T027 [P] [US2] Add transfer Server Action tests for bearer forwarding, idempotency keys, and standardized failures in `frontend/src/features/transfers/actions/__tests__/transfer.test.ts`.
- [ ] T028 [P] [US2] Add TransferForm tests for disabled pending submission and success/error rendering in `frontend/src/features/transfers/components/__tests__/TransferForm.test.tsx`.

**Checkpoint**: Transfers are concurrency-safe and retry-safe, and Story 2 integration tests prove no duplicate money movement or negative balance occurs.

---

## Phase 5: User Story 3 - Review Transaction History (Priority: P2)

**Goal**: An account holder can review only their own reverse-chronological transaction history, with pagination and shareable filters.

**Independent Test**: Seed deposits and transfers, query by every supported filter and page, reload the same URL state, and confirm no request reveals another account's history.

- [ ] T029 [P] [US3] Add filtered pagination repository tests for type, status, date range, counterparty, and out-of-range pages in `src/test/java/com/wallet/api/features/transactions/TransactionRepositoryTest.java`.
- [ ] T030 [P] [US3] Add authenticated history contract and authorization integration tests in `src/test/java/com/wallet/api/features/transactions/TransactionControllerIntegrationTest.java`.
- [ ] T031 [US3] Add validated history query and page response DTOs in `src/main/java/com/wallet/api/features/transactions/dto/TransactionHistoryQuery.java` and `src/main/java/com/wallet/api/features/transactions/dto/TransactionHistoryResponse.java`.
- [ ] T032 [US3] Implement authenticated-account-scoped, reverse-chronological filter queries in `src/main/java/com/wallet/api/features/transactions/TransactionRepository.java`.
- [ ] T033 [US3] Add history query validation and retrieval orchestration in `src/main/java/com/wallet/api/features/transactions/TransactionService.java`.
- [ ] T034 [US3] Expose documented pagination and filter parameters through `src/main/java/com/wallet/api/features/transactions/TransactionController.java`.
- [ ] T035 [US3] Bind history filters and page state to URL query parameters with nuqs in `frontend/src/features/transactions/hooks/useTransactionHistoryParams.ts`.
- [ ] T036 [US3] Add paginated transaction table, filter controls, stable loading state, and distinct empty/error states in `frontend/src/features/transactions/components/TransactionHistory.tsx`.
- [ ] T037 [US3] Add URL-state, filter, empty-state, and error-state component tests in `frontend/src/features/transactions/components/__tests__/TransactionHistory.test.tsx`.

**Checkpoint**: Story 3 returns accurate, authorized, URL-restorable history for every supported filter combination.

---

## Phase 6: User Story 4 - Use a Safe Financial Dashboard (Priority: P2)

**Goal**: The protected dashboard assembles wallet, transfer, and history workflows with layout-stable feedback throughout.

**Independent Test**: Sign in, open the dashboard under delayed and failed data requests, submit a transfer, and verify stable placeholders, disabled duplicate submission, updated balance/history, and distinct empty/error feedback.

- [ ] T038 [US4] Add the protected dashboard route, query-provider wiring, and feature composition in `frontend/src/app/dashboard/page.tsx` and `frontend/src/app/layout.tsx`.
- [ ] T039 [US4] Enforce protected-dashboard session routing in `frontend/src/proxy.ts`.
- [ ] T040 [US4] Replace text-only loading with fixed-dimension skeletons and preserve the completed balance-card layout in `frontend/src/features/accounts/components/BalanceCard.tsx`.
- [ ] T041 [US4] Add dashboard integration tests for loading, success, error, and empty states in `frontend/src/app/dashboard/__tests__/page.test.tsx`.
- [ ] T042 [US4] Add an end-to-end authenticated dashboard transfer and history-refresh scenario in `frontend/__tests__/e2e/dashboard.spec.ts`.

**Checkpoint**: Story 4 delivers a stable, protected financial dashboard across normal, loading, empty, and failure states.

---

## Phase 7: Polish and Cross-Cutting Integration

**Purpose**: Validate the complete public contract, operating workflow, and quality gates across backend and frontend.

- [ ] T043 [P] Add OpenAPI response documentation for the standardized error schema, including required `traceId`, in `src/main/java/com/wallet/api/config/ScalarApiDocsConfig.java`.
- [ ] T044 [P] Update local setup and end-to-end validation instructions in `specs/001-secure-wallet-transfers/quickstart.md`.
- [ ] T045 Run the backend integration, repository, error-contract, and concurrency suites through `mvnw` from `pom.xml`.
- [ ] T046 Run frontend lint, Vitest component suite, and Playwright dashboard suite through scripts in `frontend/package.json`.
- [ ] T047 Validate every quickstart scenario, including invalid JWTs and `traceId` correlation, against `specs/001-secure-wallet-transfers/quickstart.md`.

## Dependencies and Execution Order

- **Phase 1** has no dependencies and prepares test and local-infrastructure tooling.
- **Phase 2** depends on Phase 1 and blocks every story because auth and the unified traceable error contract are shared public behavior.
- **US1** starts after Phase 2 and is the MVP.
- **US2** depends on Phase 2 and requires the account and transaction foundations completed by US1 for its end-to-end flow.
- **US3** depends on Phase 2 and transaction records produced by US1 and US2; its backend query work can proceed once the transaction model is stable.
- **US4** depends on Phase 2 and composes the frontend work from US1-US3.
- **Phase 7** depends on the completed slices selected for release.

## Parallel Opportunities

- T001-T004 can proceed in parallel because they affect separate configuration files.
- Within each story, `[P]` test tasks can be written in parallel with one another before their dependent implementation task.
- T012 and T011, T018 and T019, and T029 and T030 can proceed in parallel because they cover separate code paths or test files.
- After Phase 2, backend query/test work and independent frontend component work may proceed in parallel where their task dependencies are met.

## Implementation Strategy

### MVP First

1. Complete Phase 1 and Phase 2.
2. Complete US1 through T017.
3. Run `AccountControllerIntegrationTest` and `BalanceCard.test.tsx` before progressing.

### Incremental Delivery

1. Deliver US1 for funded accounts and auditable deposits.
2. Add US2 and validate transfer atomicity, idempotency, and concurrency.
3. Add US3 for history review and filters.
4. Add US4 to assemble the protected dashboard.
5. Complete Phase 7 before release.
