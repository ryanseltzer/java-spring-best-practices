package learn.spring_best_practices.exception;

/**
 * Single application exception whose meaning is fully described by its {@link AppErrorCode}.
 * Throw this instead of ad-hoc RuntimeExceptions so every error is traceable back to the
 * central error-code registry.
 */
public class AppException extends RuntimeException {

    private final AppErrorCode errorCode;

    public AppException(AppErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AppErrorCode getErrorCode() {
        return errorCode;
    }
}
