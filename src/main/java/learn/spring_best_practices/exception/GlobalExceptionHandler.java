package learn.spring_best_practices.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Structured error body — no stack traces or internal detail exposed to callers (A09).
     *
     * @param appErrorCode unique application error code from {@link AppErrorCode}
     * @param httpStatus   raw HTTP status echoed for convenience
     * @param message      human-readable description
     * @param fieldErrors  per-field validation messages (non-empty only for VALIDATION_FAILED)
     * @param timestamp    UTC instant of the error
     */
    public record ErrorResponse(
            int appErrorCode,
            int httpStatus,
            String message,
            List<String> fieldErrors,
            Instant timestamp
    ) {}

    /** Path/query variable constraint violations — maps to {@link AppErrorCode#VALIDATION_FAILED} (400). */
    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        AppErrorCode code = AppErrorCode.VALIDATION_FAILED;
        List<String> fieldErrors = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(code.getAppCode(), code.getHttpStatusCode(),
                        code.getMessage(), fieldErrors, Instant.now()));
    }

    /** Bean Validation failures — maps to {@link AppErrorCode#VALIDATION_FAILED} (400). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        AppErrorCode code = AppErrorCode.VALIDATION_FAILED;
        List<String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(code.getAppCode(), code.getHttpStatusCode(),
                        code.getMessage(), fieldErrors, Instant.now()));
    }

    /** All domain errors thrown via {@link AppException} — error code drives HTTP status. */
    @ExceptionHandler(AppException.class)
    ResponseEntity<ErrorResponse> handleAppException(AppException ex) {
        AppErrorCode code = ex.getErrorCode();
        log.warn("Application error [{}]: {}", code.getAppCode(), code.getMessage());
        return ResponseEntity.status(HttpStatusCode.valueOf(code.getHttpStatusCode()))
                .body(new ErrorResponse(code.getAppCode(), code.getHttpStatusCode(),
                        code.getMessage(), List.of(), Instant.now()));
    }

    /** Safety net — never expose internal detail to the caller (A09). */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        AppErrorCode code = AppErrorCode.INTERNAL_ERROR;
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatusCode.valueOf(code.getHttpStatusCode()))
                .body(new ErrorResponse(code.getAppCode(), code.getHttpStatusCode(),
                        code.getMessage(), List.of(), Instant.now()));
    }
}
