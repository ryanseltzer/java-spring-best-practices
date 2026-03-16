package learn.spring_best_practices.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class AppErrorCodeTest {

    @ParameterizedTest
    @EnumSource(AppErrorCode.class)
    void everyCode_hasPositiveAppCode(AppErrorCode code) {
        assertThat(code.getAppCode()).isPositive();
    }

    @ParameterizedTest
    @EnumSource(AppErrorCode.class)
    void everyCode_hasValidHttpStatusCode(AppErrorCode code) {
        assertThat(code.getHttpStatusCode()).isBetween(100, 599);
    }

    @ParameterizedTest
    @EnumSource(AppErrorCode.class)
    void everyCode_hasNonBlankMessage(AppErrorCode code) {
        assertThat(code.getMessage()).isNotBlank();
    }

    @Test void validationFailed_hasExpectedValues() {
        assertThat(AppErrorCode.VALIDATION_FAILED.getAppCode()).isEqualTo(400);
        assertThat(AppErrorCode.VALIDATION_FAILED.getHttpStatusCode()).isEqualTo(400);
    }

    @Test void invalidCountry_hasExpectedValues() {
        assertThat(AppErrorCode.INVALID_COUNTRY.getAppCode()).isEqualTo(415);
        assertThat(AppErrorCode.INVALID_COUNTRY.getHttpStatusCode()).isEqualTo(422);
    }

    @Test void invalidCity_hasExpectedValues() {
        assertThat(AppErrorCode.INVALID_CITY.getAppCode()).isEqualTo(416);
        assertThat(AppErrorCode.INVALID_CITY.getHttpStatusCode()).isEqualTo(422);
    }

    @Test void invalidDateRange_hasExpectedValues() {
        assertThat(AppErrorCode.INVALID_DATE_RANGE.getAppCode()).isEqualTo(417);
        assertThat(AppErrorCode.INVALID_DATE_RANGE.getHttpStatusCode()).isEqualTo(422);
    }

    @Test void duplicateDestination_hasExpectedValues() {
        assertThat(AppErrorCode.DUPLICATE_DESTINATION.getAppCode()).isEqualTo(418);
        assertThat(AppErrorCode.DUPLICATE_DESTINATION.getHttpStatusCode()).isEqualTo(409);
    }

    @Test void internalError_hasExpectedValues() {
        assertThat(AppErrorCode.INTERNAL_ERROR.getAppCode()).isEqualTo(500);
        assertThat(AppErrorCode.INTERNAL_ERROR.getHttpStatusCode()).isEqualTo(500);
    }
}
