# Spring Best Practices

A Spring Boot REST API demonstrating practical application of the OWASP Top 10 mitigations,
structured exception handling, Bean Validation, JWT-based stateless authentication, AOP-based
observability, and Spring Boot Actuator health monitoring. The domain is intentionally simple
— storing travel destinations — so the focus stays on the plumbing rather than business complexity.

---

## Stack

- Java 25, Spring Boot 4.1.0-SNAPSHOT
- Spring Security 6 (stateless JWT filter chain)
- Spring Data JPA with H2 in-memory database
- Spring Boot Actuator (custom health indicator)
- AspectJ (`aspectjweaver`) for AOP execution logging
- jjwt 0.12.6 for JWT parsing and signature verification
- Lombok, Bean Validation (Jakarta)
- JUnit 5, Mockito, AssertJ, MockMvc
- JaCoCo 0.8.13 for test coverage reporting

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

All endpoints require a valid signed JWT in the `Authorization: Bearer <token>` header.

### POST /api/destinations/add

Adds a new travel destination.

**Request**

```
POST /api/destinations/add
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

### DELETE /api/destinations/{countryName}/{cityName}

Removes an existing destination by its composite key. The path variables are validated with the
same `@Pattern` and `@Size` constraints as the request body fields.

**Request**

```
DELETE /api/destinations/France/Paris
Authorization: Bearer <token>
```

**Responses**

| Status | Meaning |
|--------|---------|
| 200 OK | Destination deleted; response body mirrors the deleted fields |
| 400 Bad Request | Path variable failed the pattern or size constraint |
| 401 Unauthorized | JWT is missing, expired, or has an invalid signature |
| 404 Not Found | No destination exists for that country/city combination |

---

### POST /api/destinations/verify

Validates that a country and city are recognised without persisting anything. Useful for
pre-flight checks before adding a destination.

**Request**

```
POST /api/destinations/verify
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
| 200 OK | Country and city are valid; response body mirrors the request fields |
| 400 Bad Request | Bean Validation failed; `fieldErrors` array lists each violation |
| 401 Unauthorized | JWT is missing, expired, or has an invalid signature |
| 422 Unprocessable Content | Country not recognised or city name too short |

---

## Security and best practices

### A01 — Broken Access Control
Every route requires a valid JWT. The service layer performs an explicit duplicate check before
inserting, rather than relying solely on the database constraint to catch it.

### A02 — Cryptographic Failures
JWTs are signed with HMAC-SHA256. Token validity is checked in two places: jjwt's own parser
(which rejects expired or tampered tokens by throwing `JwtException`), and an explicit
comparison of the `exp` claim against `Instant.now()` inside `JwtUtil`. Tokens expire after
five minutes.

### A03 — Injection
Both `countryName` and `cityName` are restricted to the pattern `^[a-zA-Z '\-]+$` via `@Pattern`
before the method body executes, which rules out SQL metacharacters, script tags, and any other
injection payloads. This constraint is applied at both the DTO layer (`@RequestBody`) and
directly on the path variable parameters of the DELETE endpoint via `@Validated` on the
controller class. Spring Data JPA uses parameterised queries throughout, so there is no
string-concatenated SQL anywhere in the codebase.

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

`ServiceExecutionAspect` adds cross-cutting execution timing and outcome logging across the
entire service layer via AOP. Successful calls are logged at DEBUG level with elapsed time;
exceptions are logged at WARN level with the exception type and elapsed time. This keeps
timing and error observability out of the business logic itself.

---

## AOP

`ServiceExecutionAspect` is an `@Around` aspect that intercepts every method in the
`learn.spring_best_practices.service` package and its sub-packages. For each call it records:

- the short method signature
- whether the call completed normally or threw
- elapsed time in milliseconds

Successful calls emit a DEBUG log; thrown exceptions emit a WARN log and then re-throw the
original exception unchanged, so the aspect is transparent to callers.

---

## Actuator health

`DestinationServiceHealthIndicator` implements `HealthIndicator` and is exposed at:

```
GET /actuator/health/destinationService
```

The indicator performs a live `repository.count()` query on each call so it reflects real
database availability rather than just whether the bean wired correctly. The current destination
count is included in the `details` map when the status is `UP`. If the query throws, the
indicator returns `DOWN` with the exception attached.

---

## Test suite

The suite has 129 tests across 12 classes. Unit tests use plain JUnit 5 with Mockito where
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

### DestinationServiceImplTest (10 tests)
Unit tests the service layer with mocked repository and location validator. Covers: `dateFrom`
after `dateTo` throws `INVALID_DATE_RANGE` before touching any other dependency; same-day trip
is accepted; location validation failure propagates the correct error code; duplicate ID throws
`DUPLICATE_DESTINATION` without calling save; a valid request saves and maps the response
correctly; leading/trailing whitespace is trimmed before the entity is persisted;
`removeDestination` finds and deletes the entity and returns the deleted fields;
`removeDestination` throws `DESTINATION_NOT_FOUND` when the ID is absent;
`verifyDestination` returns the validated fields without writing to the repository;
`verifyDestination` propagates a location validation failure.

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

### DestinationControllerTest (20 tests)
Full-stack MockMvc tests with the real Spring Security filter chain applied via
`springSecurity()`. Tokens are generated with the same secret as `application.yaml` so the JWT
filter is properly exercised. Covers all three endpoints:

- **addDestination** (10): valid 201, same-day 201, blank country 400, XSS in country 400,
  missing dates 400, no token 401, duplicate 409, invalid country 422, invalid city 422,
  reversed date range 422.
- **verifyDestination** (6): valid 200, blank city 400, XSS in city 400, no token 401,
  invalid country 422, invalid city 422.
- **removeDestination** (4): found 200, injection in path variable 400, no token 401,
  not found 404.

### GlobalExceptionHandlerTest (10 tests)
Unit tests the exception handler directly. Covers: a single validation field error produces
a 400 with the field name and message in the array; multiple field errors are all returned;
a parameterised test runs `handleAppException` for every `AppErrorCode` enum value (7 values),
asserting that the HTTP status, app code, and message all match the enum definition; the generic
catch-all handler returns 500 with a generic message and does not echo the original exception
message back to the caller.

### AppErrorCodeTest and AppExceptionTest (35 tests)
Three parametrised tests cover every error code enum entry (7 values), verifying that each has
a positive app code, a valid HTTP status, and a non-blank message. Seven additional named tests
assert the exact app code and HTTP status for each specific enum constant. `AppExceptionTest`
verifies that `AppException` correctly stores and exposes the error code it was constructed with.

### ServiceExecutionAspectTest (2 tests)
Unit tests the AOP aspect in isolation using a mocked `ProceedingJoinPoint`. Covers: a
successful method call returns the result from `proceed()`; a throwing method call re-throws
the original exception unchanged.

### DestinationServiceHealthIndicatorTest (2 tests)
Unit tests the Actuator health indicator with a mocked repository. Covers: when the repository
is reachable the indicator returns `UP` with the live destination count in the details map;
when the repository throws the indicator returns `DOWN`.

---

## Most recent test run

Run date: 16 March 2026

```
BUILD SUCCESSFUL in 19s

learn.spring_best_practices.aop.ServiceExecutionAspectTest                     2 / 2 passed
learn.spring_best_practices.controller.DestinationControllerTest              20 / 20 passed
learn.spring_best_practices.dto.DestinationRequestValidationTest              19 / 19 passed
learn.spring_best_practices.exception.AppErrorCodeTest                        28 / 28 passed
learn.spring_best_practices.exception.AppExceptionTest                         7 / 7 passed
learn.spring_best_practices.exception.GlobalExceptionHandlerTest              10 / 10 passed
learn.spring_best_practices.health.DestinationServiceHealthIndicatorTest       2 / 2 passed
learn.spring_best_practices.repository.DestinationRepositoryTest               7 / 7 passed
learn.spring_best_practices.security.JwtAuthenticationFilterTest               6 / 6 passed
learn.spring_best_practices.security.JwtUtilTest                               6 / 6 passed
learn.spring_best_practices.service.impl.DestinationServiceImplTest           10 / 10 passed
learn.spring_best_practices.service.impl.LocationValidationServiceImplTest    12 / 12 passed

Total: 129 tests, 0 failures, 0 skipped
```

---

## Code coverage

Coverage is measured with JaCoCo 0.8.13. The report is generated automatically on every
`./gradlew test` run and written to `build/reports/jacoco/test/html/index.html`.

`config/**` and the main application entry point are excluded from metrics as they contain
Spring wiring rather than testable business logic.

| Package | Instruction coverage | Branch coverage |
|---------|---------------------|-----------------|
| `aop` | 100% | n/a |
| `controller` | 100% | n/a |
| `dto.request` | 100% | n/a |
| `dto.response` | 100% | n/a |
| `exception` | 100% | n/a |
| `health` | 100% | n/a |
| `service.impl` | 100% | 100% |
| `security` | 96% | 91% |
| **Total** | **99%** | **95%** |

The two uncovered instructions and one uncovered branch in `security` are inside
`JwtAuthenticationFilter` — they guard an edge case where a valid token carries a null subject,
which cannot occur with a well-formed HMAC-signed JWT. The current 80% minimum enforcement
threshold is configured in `jacocoTestCoverageVerification` and can be raised to match the
actual baseline.
