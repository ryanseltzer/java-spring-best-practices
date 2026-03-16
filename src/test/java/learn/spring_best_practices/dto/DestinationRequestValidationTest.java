package learn.spring_best_practices.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import learn.spring_best_practices.dto.request.DestinationRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DestinationRequestValidationTest {

    private static Validator validator;

    private static final LocalDate VALID_FROM = LocalDate.of(2026, 6, 1);
    private static final LocalDate VALID_TO   = LocalDate.of(2026, 12, 1);

    @BeforeAll
    static void buildValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    // ── Valid request ─────────────────────────────────────────────────────

    @Test
    void validRequest_hasNoViolations() {
        assertThat(validate("United Kingdom", "London", VALID_FROM, VALID_TO)).isEmpty();
    }

    // ── Country name ──────────────────────────────────────────────────────

    @Test
    void blankCountryName_hasViolation() {
        assertThat(violationFields(validate("", "London", VALID_FROM, VALID_TO)))
                .contains("countryName");
    }

    @Test
    void nullCountryName_hasViolation() {
        assertThat(violationFields(validate(null, "London", VALID_FROM, VALID_TO)))
                .contains("countryName");
    }

    @Test
    void countryNameTooLong_hasViolation() {
        String tooLong = "A".repeat(101);
        assertThat(violationFields(validate(tooLong, "London", VALID_FROM, VALID_TO)))
                .contains("countryName");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Country123", "Bad@Name", "Name!", "Name<script>"})
    void countryNameWithInvalidChars_hasViolation(String name) {
        assertThat(violationFields(validate(name, "London", VALID_FROM, VALID_TO)))
                .contains("countryName");
    }

    @ParameterizedTest
    @ValueSource(strings = {"United Kingdom", "Cote d-Ivoire", "Saint Helena"})
    void countryNameWithValidChars_hasNoViolation(String name) {
        assertThat(violationFields(validate(name, "London", VALID_FROM, VALID_TO)))
                .doesNotContain("countryName");
    }

    // ── City name ─────────────────────────────────────────────────────────

    @Test
    void blankCityName_hasViolation() {
        assertThat(violationFields(validate("United Kingdom", "", VALID_FROM, VALID_TO)))
                .contains("cityName");
    }

    @Test
    void nullCityName_hasViolation() {
        assertThat(violationFields(validate("United Kingdom", null, VALID_FROM, VALID_TO)))
                .contains("cityName");
    }

    @Test
    void cityNameTooLong_hasViolation() {
        String tooLong = "A".repeat(101);
        assertThat(violationFields(validate("United Kingdom", tooLong, VALID_FROM, VALID_TO)))
                .contains("cityName");
    }

    @ParameterizedTest
    @ValueSource(strings = {"City2", "Bad!", "City<>"})
    void cityNameWithInvalidChars_hasViolation(String city) {
        assertThat(violationFields(validate("United Kingdom", city, VALID_FROM, VALID_TO)))
                .contains("cityName");
    }

    // ── Dates ─────────────────────────────────────────────────────────────

    @Test
    void nullDateFrom_hasViolation() {
        assertThat(violationFields(validate("United Kingdom", "London", null, VALID_TO)))
                .contains("dateFrom");
    }

    @Test
    void nullDateTo_hasViolation() {
        assertThat(violationFields(validate("United Kingdom", "London", VALID_FROM, null)))
                .contains("dateTo");
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Set<ConstraintViolation<DestinationRequest>> validate(
            String country, String city, LocalDate from, LocalDate to) {
        DestinationRequest req = new DestinationRequest();
        req.setCountryName(country);
        req.setCityName(city);
        req.setDateFrom(from);
        req.setDateTo(to);
        return validator.validate(req);
    }

    private Set<String> violationFields(Set<ConstraintViolation<DestinationRequest>> violations) {
        var fields = new java.util.HashSet<String>();
        violations.forEach(v -> fields.add(v.getPropertyPath().toString()));
        return fields;
    }
}
