package learn.spring_best_practices.exception;

/**
 * Central registry of all application error codes.
 *
 * Each entry carries:
 *   appCode        – unique integer returned in the response body
 *   httpStatusCode – raw HTTP status code the handler will use (avoids deprecated HttpStatus constants)
 *   message        – default human-readable message
 *
 * App-code ranges:
 *   4xx  – client / input errors
 *   5xx  – server errors
 */
public enum AppErrorCode {

    // ── Input / Validation ────────────────────────────────────────
    VALIDATION_FAILED       (400, 400, "Request validation failed"),

    // ── Location errors (HTTP 422 Unprocessable Content) ──────────
    INVALID_COUNTRY         (415, 422, "The provided country is not recognised as a valid country"),
    INVALID_CITY            (416, 422, "The provided city name is invalid"),
    INVALID_DATE_RANGE      (417, 422, "dateFrom must not be after dateTo"),

    // ── Resource conflicts (HTTP 409 Conflict) ────────────────────
    DUPLICATE_DESTINATION   (418, 409, "A destination for that country and city already exists"),

    // ── Server errors ─────────────────────────────────────────────
    INTERNAL_ERROR          (500, 500, "An internal server error occurred");

    private final int appCode;
    private final int httpStatusCode;
    private final String message;

    AppErrorCode(int appCode, int httpStatusCode, String message) {
        this.appCode = appCode;
        this.httpStatusCode = httpStatusCode;
        this.message = message;
    }

    public int getAppCode()       { return appCode; }
    public int getHttpStatusCode(){ return httpStatusCode; }
    public String getMessage()    { return message; }
}
