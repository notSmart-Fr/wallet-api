<!--
Sync Impact Report
Version change: scaffold -> 1.0.0
Modified principles: none (initial adoption)
Added sections: Core Principles, Engineering Constraints, Delivery Workflow, Governance
Removed sections: none
Follow-up TODOs: none
-->
# Wallet API Constitution

## Core Principles

### I. Self-Contained Domain Architecture
Each business domain MUST own its models, use cases, persistence boundaries, and public contracts.
Cross-domain access MUST occur through immutable, explicitly versioned contracts rather than direct
access to another domain's internal state. Every domain boundary MUST validate its inputs and reject
invalid states before they can reach persistence or external integrations. This preserves business
invariants, enables independent evolution, and makes ownership clear.

### II. Financial Precision and Transaction Safety
All currency amounts MUST use arbitrary-precision decimal representations with an explicit currency
and rounding policy; binary floating-point arithmetic is prohibited for monetary values. Balance and
ledger mutations MUST execute within one atomic database transaction. Concurrent mutations of shared
financial records MUST acquire pessimistic row locks in one documented deterministic order. This
protects money from precision loss, partial writes, races, and deadlocks.

### III. Predictable User Experience
Every externally visible failure MUST use the standardized, unified error payload and MUST avoid
leaking implementation details. User-controllable query state MUST be represented in the URL so a
view is shareable, restorable, and navigable. Loading, error, and empty UI fallbacks MUST reserve the
final layout's space to prevent layout shift. These rules make financial workflows understandable,
recoverable, and stable.

### IV. Boundary Security and Efficient Authorization
Authorization MUST use stateless cryptographically signed tokens that are verified at every protected
service boundary. Session or authentication protection MUST be applied at application boundaries
before protected data, routes, or actions execute. Token validation and boundary checks MUST be
constant with respect to application state where practical and MUST avoid unnecessary session
lookups. This keeps authorization scalable while preventing unauthenticated access.

## Engineering Constraints

Domain contracts MUST be immutable after publication; a compatible extension or an explicit new
version is required for contract changes. Financial write paths MUST include tests covering precision,
rounding, rollback, concurrent access, and deterministic lock ordering. Public error payloads MUST be
tested as contracts. Security-sensitive endpoints and protected UI entry points MUST demonstrate that
missing, expired, malformed, or invalid tokens are denied.

## Delivery Workflow

Each proposed change MUST identify the affected domain, its contracts, validation rules, and financial
or security implications before implementation. Code review MUST verify compliance with every
applicable core principle. Changes that alter financial state, public contracts, authorization, or
error payloads MUST include focused automated tests and an explicit rollback or migration plan where
data compatibility is affected. A noncompliant exception requires a written, time-bounded rationale
and approval from the project maintainers.

## Governance

This constitution supersedes conflicting development practices. Amendments require a documented
proposal that identifies affected principles, implementation consequences, and any migration work;
project maintainers approve the amendment before it is adopted. Compliance MUST be reviewed in pull
requests and before releases for affected areas.

Constitution versions use semantic versioning: MAJOR for backward-incompatible governance changes,
MINOR for a new principle or materially expanded obligation, and PATCH for clarifications that retain
the existing obligations. The ratification date records the first adoption; the last-amended date is
updated whenever the constitution changes.

**Version**: 1.0.0 | **Ratified**: 2026-09-07 | **Last Amended**: 2026-09-07
