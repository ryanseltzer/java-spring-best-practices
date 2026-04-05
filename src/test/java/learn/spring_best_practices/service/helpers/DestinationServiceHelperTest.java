package learn.spring_best_practices.service.helpers;

import learn.spring_best_practices.dto.request.DestinationRequest;
import learn.spring_best_practices.entity.DestinationId;
import learn.spring_best_practices.exception.AppErrorCode;
import learn.spring_best_practices.exception.AppException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DestinationServiceHelperTest {

    private static final LocalDate DATE_FROM = LocalDate.of(2025, 6, 1);
    private static final LocalDate DATE_TO   = LocalDate.of(2025, 6, 15);

    // ── validateDateRange ─────────────────────────────────────────────────

    @Test
    void validateDateRange_dateFromAfterDateTo_throwsInvalidDateRange() {
        assertThatThrownBy(() -> DestinationServiceHelper.validateDateRange(DATE_TO, DATE_FROM))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(AppErrorCode.INVALID_DATE_RANGE));
    }

    @Test
    void validateDateRange_dateFromEqualDateTo_doesNotThrow() {
        DestinationServiceHelper.validateDateRange(DATE_FROM, DATE_FROM);
    }

    @Test
    void validateDateRange_validRange_doesNotThrow() {
        DestinationServiceHelper.validateDateRange(DATE_FROM, DATE_TO);
    }

    // ── buildId ───────────────────────────────────────────────────────────

    @Test
    void buildId_plainInput_returnsCorrectId() {
        DestinationRequest req = buildRequest("France", "Paris");

        DestinationId id = DestinationServiceHelper.buildId(req);

        assertThat(id.getCountryName()).isEqualTo("France");
        assertThat(id.getCityName()).isEqualTo("Paris");
    }

    @Test
    void buildId_whitespacepadded_trimsCountryAndCity() {
        DestinationRequest req = buildRequest("  United Kingdom  ", "  London  ");

        DestinationId id = DestinationServiceHelper.buildId(req);

        assertThat(id.getCountryName()).isEqualTo("United Kingdom");
        assertThat(id.getCityName()).isEqualTo("London");
    }

    @Test
    void buildId_leadingWhitespaceOnly_trimsCorrectly() {
        DestinationRequest req = buildRequest("  Germany", "  Berlin");

        DestinationId id = DestinationServiceHelper.buildId(req);

        assertThat(id.getCountryName()).isEqualTo("Germany");
        assertThat(id.getCityName()).isEqualTo("Berlin");
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private DestinationRequest buildRequest(String country, String city) {
        DestinationRequest req = new DestinationRequest();
        req.setCountryName(country);
        req.setCityName(city);
        req.setDateFrom(DATE_FROM);
        req.setDateTo(DATE_TO);
        return req;
    }
}
