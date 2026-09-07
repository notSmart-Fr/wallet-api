# API Contract: Standardized Error Payload

All error responses across every endpoint in this feature use this single JSON shape.

```json
{
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Deposit amount must be a positive value with at most 4 decimal places.",
  "timestamp": "2026-09-07T12:34:56.789Z",
  "traceId": "7c908f06-059b-4dbe-9a1c-77d30fa1e0b3"
}
```

| Field | Type | Description |
|---|---|---|
| `status` | integer | HTTP status code |
| `error` | string | Stable machine-readable error code (see table below) |
| `message` | string | User-safe, human-readable message; MUST NOT leak stack traces, SQL, or internal identifiers |
| `timestamp` | string (ISO-8601) | Server time the error was generated |
| `traceId` | string | Required non-empty opaque identifier that uniquely correlates the failed request end to end; it MAY be shared with support and MUST NOT contain personal data or internal diagnostics |

## Error Codes

| `error` code | HTTP status | Triggered by |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Invalid/zero/negative amount, unsupported precision, malformed request body |
| `UNAUTHENTICATED` | 401 | Missing, expired, or malformed JWT |
| `FORBIDDEN` | 403 | Authenticated but not authorized for the requested resource (e.g., another account's history) |
| `ACCOUNT_NOT_FOUND` | 404 | Recipient account identifier does not exist |
| `SELF_TRANSFER_NOT_ALLOWED` | 422 | Sender and recipient identifiers are the same account |
| `INSUFFICIENT_FUNDS` | 422 | Sender balance is less than the requested transfer amount |
| `DUPLICATE_SUBMISSION` | 409 | Idempotency key was already used for a completed transfer; original result is returned instead of re-executing |
| `INTERNAL_ERROR` | 500 | Unexpected/unhandled failure |

This contract is produced by the centralized `@RestControllerAdvice` (`GlobalExceptionHandler`) and
is considered part of the tested public contract per the constitution's Engineering Constraints.
