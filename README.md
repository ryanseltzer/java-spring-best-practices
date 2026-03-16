# Spring Best Practices

A Spring Boot REST API demonstrating practical application of the OWASP Top 10 mitigations,
structured exception handling, Bean Validation, and JWT-based stateless authentication. The
domain is intentionally simple — storing travel destinations — so the focus stays on the
plumbing rather than business complexity.

---

## Stack

- Java 25, Spring Boot 4.1.0-SNAPSHOT
- Spring Security 6 (stateless JWT filter chain)
- Spring Data JPA with H2 in-memory database
- jjwt 0.12.6 for JWT parsing and signature verification
- Lombok, Bean Validation (Jakarta)
- JUnit 5, Mockito, AssertJ, MockMvc

---

## Running the application

Copy `application.yaml.example` to `src/main/resources/application.yaml` and fill in your own
JWT secret before starting. The secret must be a Base64-encoded value of at least 256 bits.
You can generate one with:

```
openssl rand -base64 32
```

Then start the app:

```
./gradlew bootRun
```

The server starts on port 8080. The H2 console is available at `/h2-console` and is intended
for local development only — it must be disabled before any deployment.

---

## Endpoints

### POST /api/destinations

Adds a new travel destination. All requests must include a valid signed JWT in the
`Authorization` header.

**Request**

```
POST /api/destinations
Authorization: Bearer <token>
Content-Type: application/json

{
  "countryName": "France",
  "cityName": "Paris",
  "dateFrom": "2025-06-01",
  "dateTo": "2025-06-10"
}
```

**Responses**

| Status | Meaning |
|--------|---------|
| 201 Created | Destination saved; response body mirrors the saved fields |
| 400 Bad Request | Bean Validation failed; `fieldErrors` array lists each violation |
| 401 Unauthorized | JWT is missing, expired, or has an invalid signature |
| 409 Conflict | A destination for that country/city combination already exists |
| 422 Unprocessable Content | Country not recognised, city name too short, or dateFrom is after dateTo |
| 500 Internal Server Error | Unhandled server fault; no internal detail is returned to the caller |

---

## Security and best practices

### A01 — Broken Access Control
Every route except the H2 console requires a valid JWT. The service layer performs an explicit
duplicate check before inserting, rather than relying solely on the database constraint to catch
it.

### A02 — Cryptographic Failures
JWTs are signed with HMAC-SHA256. Token validity is checked in two places: jjwt's own parser
(which rejects expired or tampered tokens by throwing `JwtException`), and an explicit
comparison of the `exp` claim against `Instant.now()` inside `JwtUtil`. Tokens expire after
five minutes.

### A03 — Injection
Both `countryName` and `cityName` are restricted to the pattern `^[a-zA-Z '\-]+$` via `@Pattern`
before the method body executes, which rules out SQL metacharacters, script tags, and any other
injection payloads at the DTO layer. Spring Data JPA uses parameterised queries throughout, so
there is no string-concatenated SQL anywhere in the codebase.

### A04 — Insecure Design
Date range validation, location validation, and duplicate detection all run as separate,
testable concerns before any write to the database occurs. Same-day trips (`dateFrom == dateTo`)
are explicitly supported.

### A05 — Security Misconfiguration
HTTP response headers are hardened in `SecurityConfig`: `X-Content-Type-Options`,
`Strict-Transport-Security` (with `includeSubDomains`, one-year max-age), and same-origin
frame policy for the H2 console iframe. CSRF is disabled because the API is stateless and
authenticated via JWT rather than session cookies.

### A07 — Identification and Authentication Failures
Sessions are stateless (`SessionCreationPolicy.STATELESS`). The JWT filter is inserted before
Spring's default `UsernamePasswordAuthenticationFilter`. Requests without a `Bearer` prefix
pass through the filter without touching the JWT library — they will be rejected downstream
by the `authenticated()` authorisation rule, which returns a plain 401 rather than redirecting
to a login page.

### A08 — Software and Data Integrity Failures
`@RequestBody` only deserialises the fields declared on `DestinationRequest`, so extra fields
in the JSON payload are silently ignored and cannot bind to unexpected properties.

### A09 — Security Logging and Monitoring Failures
User-supplied input is never echoed directly into log output. Authentication failures are logged
at WARN level with the remote address. Unhandled exceptions are logged at ERROR level with the
full stack trace server-side, but the response body contains only a generic message — no stack
trace, exception class, or internal detail is ever returned to the caller.

---

## Test suite

The suite has 106 tests across 11 classes. Unit tests use plain JUnit 5 with Mockito where
dependencies need to be isolated. Integration tests load the full Spring context against a real
H2 database with transactional rollback, so there is no shared state between runs. Controller
tests use real signed JWTs rather than `@WithMockUser` so that the JWT filter chain is exercised
end-to-end.

### JwtUtilTest (6 tests)
Directly tests the `JwtUtil` component by injecting the secret via `ReflectionTestUtils`.
Covers a valid token returning `true`, an expired token returning `false`, a tampered signature
returning `false`, garbage input, an empty string, and correct subject extraction.

### JwtAuthenticationFilterTest (6 tests)
Unit tests the servlet filter in isolation. Covers: no `Authorization` header passes through
without calling the JWT library; a non-Bearer scheme (e.g. Basic) is ignored; an invalid token
sends a 401 and halts the filter chain; a valid token with a subject sets the `SecurityContext`;
a valid token with a null subject does not set authentication; a valid token does not overwrite
an already-authenticated context.

### DestinationRequestValidationTest (19 tests)
Exercises the Bean Validation constraints directly, without any Spring context. Covers blank,
null, and oversized values for both `countryName` and `cityName`, parameterised tests for
invalid characters (digits, `@`, `!`, `<script>`), parameterised tests for valid multi-word
country names, and null values for both date fields.

### DestinationServiceImplTest (6 tests)
Unit tests the service layer with mocked repository and location validator. Covers: `dateFrom`
after `dateTo` throws `INVALID_DATE_RANGE` before touching any other dependency; same-day trip
is accepted; location validation failure propagates the correct error code; duplicate ID throws
`DUPLICATE_DESTINATION` without calling save; a valid request saves and maps the response
correctly; leading/trailing whitespace is trimmed before the entity is persisted.

### LocationValidationServiceImplTest (12 tests)
Tests the country/city validator in isolation. Covers known countries passing, whitespace-padded
country names passing, five parameterised valid countries, an unrecognised country throwing
`INVALID_COUNTRY`, an empty country string, a one-character city throwing `INVALID_CITY`, an
empty city, and a whitespace-only city.

### DestinationRepositoryTest (7 tests)
Integration tests against a live H2 database. Each test runs in a transaction that rolls back
on completion. Covers: save persists with correct fields; `findById` returns the entity when
it exists; `findById` returns empty when it does not; `existsById` returns true after save;
`existsById` returns false when absent; `findAll` returns all saved rows; `deleteById` removes
the record.

### DestinationControllerTest (10 tests)
Full-stack MockMvc tests with the real Spring Security filter chain applied via
`springSecurity()`. Tokens are generated with the same secret as `application.yaml` so the JWT
filter is properly exercised. Covers: valid request returns 201 with the correct JSON body;
same-day trip returns 201; blank country returns 400 with `fieldErrors`; XSS payload in country
returns 400; missing dates return 400; no token returns 401; duplicate throws 409 with app code
418; unrecognised country returns 422 with app code 415; invalid city returns 422 with app code
416; reversed date range returns 422 with app code 417.

### GlobalExceptionHandlerTest (9 tests)
Unit tests the exception handler directly. Covers: a single validation field error produces
a 400 with the field name and message in the array; multiple field errors are all returned;
a parameterised test runs `handleAppException` for every `AppErrorCode` enum value, asserting
that the HTTP status, app code, and message all match the enum definition; the generic catch-all
handler returns 500 with a generic message and does not echo the original exception message back
to the caller.

### AppErrorCodeTest and AppExceptionTest (30 tests)
Verify that every error code enum entry has the expected app code, HTTP status, and message,
and that `AppException` correctly stores and exposes the error code it was constructed with.

---

## Most recent test run

Run date: 16 March 2026

```
BUILD SUCCESSFUL in 20s

learn.spring_best_practices.ApplicationTests                     1 / 1 passed
learn.spring_best_practices.controller.DestinationControllerTest    10 / 10 passed
learn.spring_best_practices.dto.DestinationRequestValidationTest    19 / 19 passed
learn.spring_best_practices.exception.AppErrorCodeTest              24 / 24 passed
learn.spring_best_practices.exception.AppExceptionTest               6 / 6 passed
learn.spring_best_practices.exception.GlobalExceptionHandlerTest     9 / 9 passed
learn.spring_best_practices.repository.DestinationRepositoryTest     7 / 7 passed
learn.spring_best_practices.security.JwtAuthenticationFilterTest     6 / 6 passed
learn.spring_best_practices.security.JwtUtilTest                     6 / 6 passed
learn.spring_best_practices.service.impl.DestinationServiceImplTest  6 / 6 passed
learn.spring_best_practices.service.impl.LocationValidationServiceImplTest  12 / 12 passed

Total: 106 tests, 0 failures, 0 skipped
```
