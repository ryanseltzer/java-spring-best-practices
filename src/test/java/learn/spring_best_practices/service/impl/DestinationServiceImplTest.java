package learn.spring_best_practices.service.impl;

import learn.spring_best_practices.dto.request.DestinationRequest;
import learn.spring_best_practices.dto.response.DestinationResponse;
import learn.spring_best_practices.entity.Destination;
import learn.spring_best_practices.entity.DestinationId;
import learn.spring_best_practices.exception.AppErrorCode;
import learn.spring_best_practices.exception.AppException;
import learn.spring_best_practices.repository.DestinationRepository;
import learn.spring_best_practices.service.LocationValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DestinationServiceImplTest {

    @Mock DestinationRepository destinationRepository;
    @Mock LocationValidationService locationValidationService;
    @InjectMocks DestinationServiceImpl service;

    private static final LocalDate DATE_FROM = LocalDate.of(2026, 6, 1);
    private static final LocalDate DATE_TO   = LocalDate.of(2026, 12, 1);

    // ── Date range validation ─────────────────────────────────────────────

    @Test
    void addDestination_dateFromAfterDateTo_throwsInvalidDateRange() {
        DestinationRequest req = buildRequest("United Kingdom", "London", DATE_TO, DATE_FROM);

        assertThatThrownBy(() -> service.addDestination(req))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(AppErrorCode.INVALID_DATE_RANGE));

        verifyNoInteractions(locationValidationService, destinationRepository);
    }

    @Test
    void addDestination_dateFromEqualDateTo_isValidSameDayTrip() {
        // A single-day trip (dateFrom == dateTo) is explicitly supported
        DestinationRequest req = buildRequest("United Kingdom", "London", DATE_FROM, DATE_FROM);
        DestinationId id = new DestinationId("United Kingdom", "London");
        Destination saved = new Destination(id, DATE_FROM, DATE_FROM);

        when(destinationRepository.existsById(any())).thenReturn(false);
        when(destinationRepository.save(any())).thenReturn(saved);

        DestinationResponse response = service.addDestination(req);

        assertThat(response.dateFrom()).isEqualTo(response.dateTo());
    }

    // ── Location validation ───────────────────────────────────────────────

    @Test
    void addDestination_locationValidationFails_propagatesException() {
        DestinationRequest req = buildRequest("Narnia", "Castle", DATE_FROM, DATE_TO);
        doThrow(new AppException(AppErrorCode.INVALID_COUNTRY))
                .when(locationValidationService).validateLocation(any(), any());

        assertThatThrownBy(() -> service.addDestination(req))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(AppErrorCode.INVALID_COUNTRY));

        verifyNoInteractions(destinationRepository);
    }

    // ── Duplicate check ───────────────────────────────────────────────────

    @Test
    void addDestination_duplicateDestination_throwsDuplicate() {
        DestinationRequest req = validRequest();
        when(destinationRepository.existsById(any())).thenReturn(true);

        assertThatThrownBy(() -> service.addDestination(req))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(AppErrorCode.DUPLICATE_DESTINATION));

        verify(destinationRepository, never()).save(any());
    }

    // ── Success ───────────────────────────────────────────────────────────

    @Test
    void addDestination_validRequest_savesAndReturnsResponse() {
        DestinationRequest req = validRequest();
        DestinationId id = new DestinationId("United Kingdom", "London");
        Destination saved = new Destination(id, DATE_FROM, DATE_TO);

        when(destinationRepository.existsById(any())).thenReturn(false);
        when(destinationRepository.save(any())).thenReturn(saved);

        DestinationResponse response = service.addDestination(req);

        assertThat(response.countryName()).isEqualTo("United Kingdom");
        assertThat(response.cityName()).isEqualTo("London");
        assertThat(response.dateFrom()).isEqualTo(DATE_FROM);
        assertThat(response.dateTo()).isEqualTo(DATE_TO);

        verify(destinationRepository).save(any(Destination.class));
        verify(locationValidationService).validateLocation("United Kingdom", "London");
    }

    @Test
    void addDestination_trimsWhitespaceBeforePersisting() {
        DestinationRequest req = buildRequest("  United Kingdom  ", "  London  ", DATE_FROM, DATE_TO);
        DestinationId id = new DestinationId("United Kingdom", "London");
        Destination saved = new Destination(id, DATE_FROM, DATE_TO);

        when(destinationRepository.existsById(any())).thenReturn(false);
        when(destinationRepository.save(any())).thenReturn(saved);

        DestinationResponse response = service.addDestination(req);

        assertThat(response.countryName()).isEqualTo("United Kingdom");
        assertThat(response.cityName()).isEqualTo("London");
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private DestinationRequest validRequest() {
        return buildRequest("United Kingdom", "London", DATE_FROM, DATE_TO);
    }

    private DestinationRequest buildRequest(String country, String city,
                                            LocalDate from, LocalDate to) {
        DestinationRequest req = new DestinationRequest();
        req.setCountryName(country);
        req.setCityName(city);
        req.setDateFrom(from);
        req.setDateTo(to);
        return req;
    }
}
