package learn.spring_best_practices.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // ── Validation failures ──────────────────────────────────────────────

    @Test
    void handleValidation_returnsFieldErrorsAnd400() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("destinationRequest", "countryName", "is required");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().appErrorCode()).isEqualTo(AppErrorCode.VALIDATION_FAILED.getAppCode());
        assertThat(response.getBody().fieldErrors()).containsExactly("countryName: is required");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void handleValidation_multipleFieldErrors_allReturned() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        List<FieldError> errors = List.of(
                new FieldError("obj", "countryName", "is required"),
                new FieldError("obj", "cityName", "is required")
        );

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(errors);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleValidation(ex);

        assertThat(response.getBody().fieldErrors()).hasSize(2);
    }

    // ── AppException (domain errors) ─────────────────────────────────────

    @ParameterizedTest
    @EnumSource(AppErrorCode.class)
    void handleAppException_mapsHttpStatusFromErrorCode(AppErrorCode code) {
        AppException ex = new AppException(code);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleAppException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(code.getHttpStatusCode());
        assertThat(response.getBody().appErrorCode()).isEqualTo(code.getAppCode());
        assertThat(response.getBody().httpStatus()).isEqualTo(code.getHttpStatusCode());
        assertThat(response.getBody().message()).isEqualTo(code.getMessage());
        assertThat(response.getBody().fieldErrors()).isEmpty();
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    // ── Generic catch-all ─────────────────────────────────────────────────

    @Test
    void handleGeneric_returns500WithGenericMessage() {
        RuntimeException ex = new RuntimeException("Something broke internally");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleGeneric(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().appErrorCode()).isEqualTo(AppErrorCode.INTERNAL_ERROR.getAppCode());
        assertThat(response.getBody().message()).isEqualTo(AppErrorCode.INTERNAL_ERROR.getMessage());
        // Internal detail must NOT be echoed to the caller
        assertThat(response.getBody().message()).doesNotContain("Something broke internally");
    }
}
