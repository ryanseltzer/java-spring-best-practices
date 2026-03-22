package learn.spring_best_practices.service.impl;

import learn.spring_best_practices.dto.request.DestinationListRequest;
import learn.spring_best_practices.dto.request.DestinationRequest;
import learn.spring_best_practices.dto.request.RemoveDestinationRequest;
import learn.spring_best_practices.dto.response.DestinationListResponse;
import learn.spring_best_practices.dto.response.DestinationResponse;
import learn.spring_best_practices.entity.Destination;
import learn.spring_best_practices.entity.DestinationId;
import learn.spring_best_practices.exception.AppErrorCode;
import learn.spring_best_practices.exception.AppException;
import learn.spring_best_practices.repository.DestinationRepository;
import learn.spring_best_practices.service.LocationValidationService;
import java.util.List;
import java.util.Optional;
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

    // ── removeDestination ─────────────────────────────────────────────────

    @Test
    void removeDestination_found_deletesAndReturnsResponse() {
        RemoveDestinationRequest request = new RemoveDestinationRequest("United Kingdom", "London");
        DestinationId id = new DestinationId("United Kingdom", "London");
        Destination existing = new Destination(id, DATE_FROM, DATE_TO);

        when(destinationRepository.findById(id)).thenReturn(Optional.of(existing));

        DestinationResponse response = service.removeDestination(request);

        assertThat(response.countryName()).isEqualTo("United Kingdom");
        assertThat(response.cityName()).isEqualTo("London");
        assertThat(response.dateFrom()).isEqualTo(DATE_FROM);
        assertThat(response.dateTo()).isEqualTo(DATE_TO);

        verify(destinationRepository).delete(existing);
    }

    @Test
    void removeDestination_notFound_throwsDestinationNotFound() {
        RemoveDestinationRequest request = new RemoveDestinationRequest("United Kingdom", "London");
        DestinationId id = new DestinationId("United Kingdom", "London");
        when(destinationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeDestination(request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(AppErrorCode.DESTINATION_NOT_FOUND));

        verify(destinationRepository, never()).delete(any());
    }

    // ── verifyDestination ─────────────────────────────────────────────────

    @Test
    void verifyDestination_validRequest_returnsResponse() {
        DestinationRequest req = validRequest();

        DestinationResponse response = service.verifyDestination(req);

        assertThat(response.countryName()).isEqualTo("United Kingdom");
        assertThat(response.cityName()).isEqualTo("London");
        assertThat(response.dateFrom()).isEqualTo(DATE_FROM);
        assertThat(response.dateTo()).isEqualTo(DATE_TO);

        verify(locationValidationService).validateLocation("United Kingdom", "London");
        verifyNoInteractions(destinationRepository);
    }

    @Test
    void verifyDestination_locationValidationFails_propagatesException() {
        DestinationRequest req = buildRequest("Narnia", "Castle", DATE_FROM, DATE_TO);
        doThrow(new AppException(AppErrorCode.INVALID_COUNTRY))
                .when(locationValidationService).validateLocation(any(), any());

        assertThatThrownBy(() -> service.verifyDestination(req))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(AppErrorCode.INVALID_COUNTRY));

        verifyNoInteractions(destinationRepository);
    }

    // ── listDestinations ──────────────────────────────────────────────────

    @Test
    void listDestinations_dateFromAfterDateTo_throwsInvalidDateRange() {
        DestinationListRequest req = listRequest(DATE_TO, DATE_FROM);

        assertThatThrownBy(() -> service.listDestinations(req))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(AppErrorCode.INVALID_DATE_RANGE));

        verifyNoInteractions(destinationRepository);
    }

    @Test
    void listDestinations_validRange_returnsMatchingDestinations() {
        DestinationId id = new DestinationId("United Kingdom", "London");
        Destination dest = new Destination(id, DATE_FROM, DATE_TO);
        when(destinationRepository.findByDateRangeOverlap(DATE_FROM, DATE_TO)).thenReturn(List.of(dest));

        DestinationListResponse response = service.listDestinations(listRequest(DATE_FROM, DATE_TO));

        assertThat(response.destinations()).hasSize(1);
        assertThat(response.destinations().get(0).countryName()).isEqualTo("United Kingdom");
        assertThat(response.destinations().get(0).cityName()).isEqualTo("London");
        assertThat(response.count()).isEqualTo(1);
    }

    @Test
    void listDestinations_noResultsInRange_returnsEmptyListWithZeroCount() {
        when(destinationRepository.findByDateRangeOverlap(any(), any())).thenReturn(List.of());

        DestinationListResponse response = service.listDestinations(listRequest(DATE_FROM, DATE_TO));

        assertThat(response.destinations()).isEmpty();
        assertThat(response.count()).isZero();
    }

    @Test
    void listDestinations_multipleResults_allMappedCorrectly() {
        Destination dest1 = new Destination(new DestinationId("United Kingdom", "London"), DATE_FROM, DATE_TO);
        Destination dest2 = new Destination(new DestinationId("France", "Paris"), DATE_FROM, DATE_TO);
        when(destinationRepository.findByDateRangeOverlap(DATE_FROM, DATE_TO)).thenReturn(List.of(dest1, dest2));

        DestinationListResponse response = service.listDestinations(listRequest(DATE_FROM, DATE_TO));

        assertThat(response.count()).isEqualTo(2);
        assertThat(response.destinations()).extracting("countryName")
                .containsExactly("United Kingdom", "France");
    }

    @Test
    void listDestinations_sameDayRange_isValid() {
        when(destinationRepository.findByDateRangeOverlap(DATE_FROM, DATE_FROM)).thenReturn(List.of());

        assertThat(service.listDestinations(listRequest(DATE_FROM, DATE_FROM)).destinations()).isEmpty();
        verify(destinationRepository).findByDateRangeOverlap(DATE_FROM, DATE_FROM);
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

    private DestinationListRequest listRequest(LocalDate from, LocalDate to) {
        DestinationListRequest req = new DestinationListRequest();
        req.setDateFrom(from);
        req.setDateTo(to);
        return req;
    }
}
