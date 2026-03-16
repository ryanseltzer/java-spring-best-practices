package learn.spring_best_practices.service.impl;

import learn.spring_best_practices.exception.AppErrorCode;
import learn.spring_best_practices.exception.AppException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

class LocationValidationServiceImplTest {

    private final LocationValidationServiceImpl service = new LocationValidationServiceImpl();

    // ── Valid locations ───────────────────────────────────────────────────

    @Test
    void validateLocation_knownCountryAndCity_passes() {
        assertThatNoException()
                .isThrownBy(() -> service.validateLocation("United Kingdom", "London"));
    }

    @Test
    void validateLocation_countryWithLeadingSpaces_passes() {
        assertThatNoException()
                .isThrownBy(() -> service.validateLocation("  France  ", "Paris"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"United States", "Germany", "Japan", "Australia", "Brazil"})
    void validateLocation_variousValidCountries_pass(String country) {
        assertThatNoException()
                .isThrownBy(() -> service.validateLocation(country, "SomeCity"));
    }

    // ── Invalid country ───────────────────────────────────────────────────

    @Test
    void validateLocation_unrecognisedCountry_throwsInvalidCountry() {
        assertThatThrownBy(() -> service.validateLocation("Narnia", "Castle"))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(AppErrorCode.INVALID_COUNTRY));
    }

    @Test
    void validateLocation_emptyCountry_throwsInvalidCountry() {
        assertThatThrownBy(() -> service.validateLocation("", "London"))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(AppErrorCode.INVALID_COUNTRY));
    }

    // ── Invalid city ──────────────────────────────────────────────────────

    @Test
    void validateLocation_cityOfOneChar_throwsInvalidCity() {
        assertThatThrownBy(() -> service.validateLocation("United Kingdom", "A"))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(AppErrorCode.INVALID_CITY));
    }

    @Test
    void validateLocation_emptyCity_throwsInvalidCity() {
        assertThatThrownBy(() -> service.validateLocation("United Kingdom", ""))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(AppErrorCode.INVALID_CITY));
    }

    @Test
    void validateLocation_cityWithOnlySpaces_throwsInvalidCity() {
        assertThatThrownBy(() -> service.validateLocation("United Kingdom", " "))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(AppErrorCode.INVALID_CITY));
    }
}
