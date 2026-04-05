package learn.spring_best_practices.service.impl;

import learn.spring_best_practices.dto.request.DestinationListRequest;
import learn.spring_best_practices.dto.request.DestinationRequest;
import learn.spring_best_practices.dto.request.RemoveDestinationRequest;
import learn.spring_best_practices.entity.Destination;
import learn.spring_best_practices.entity.DestinationId;
import learn.spring_best_practices.repository.DestinationRepository;
import learn.spring_best_practices.service.DestinationService;
import learn.spring_best_practices.service.LocationValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Verifies that the Spring Cache proxy around {@link DestinationService#listDestinations}
 * behaves correctly: cache hits avoid the repository, and write operations evict stale entries.
 *
 * Requires a real Spring context so the {@code @Cacheable} / {@code @CacheEvict} AOP proxies
 * are active. Tests run sequentially to prevent concurrent stub overwrites on shared mocks.
 */
@SpringBootTest
@Execution(ExecutionMode.SAME_THREAD)
class DestinationServiceCachingTest {

    @Autowired DestinationService destinationService;
    @Autowired CacheManager cacheManager;

    @MockitoBean DestinationRepository destinationRepository;
    @MockitoBean LocationValidationService locationValidationService;

    private static final LocalDate DATE_FROM = LocalDate.of(2026, 6, 1);
    private static final LocalDate DATE_TO   = LocalDate.of(2026, 12, 1);

    @BeforeEach
    void clearCache() {
        cacheManager.getCache("destinations").clear();
    }

    // ── @Cacheable — cache hits ───────────────────────────────────────────

    @Test
    void listDestinations_calledTwiceWithSameRange_onlyHitsRepositoryOnce() {
        when(destinationRepository.findByDateRangeOverlap(any(), any())).thenReturn(List.of());

        destinationService.listDestinations(listRequest(DATE_FROM, DATE_TO));
        destinationService.listDestinations(listRequest(DATE_FROM, DATE_TO));

        verify(destinationRepository, times(1)).findByDateRangeOverlap(DATE_FROM, DATE_TO);
    }

    @Test
    void listDestinations_differentDateRanges_hitRepositoryForEachKey() {
        when(destinationRepository.findByDateRangeOverlap(any(), any())).thenReturn(List.of());

        destinationService.listDestinations(listRequest(DATE_FROM, DATE_TO));
        destinationService.listDestinations(listRequest(DATE_FROM.plusMonths(1), DATE_TO));

        verify(destinationRepository, times(2)).findByDateRangeOverlap(any(), any());
    }

    // ── @CacheEvict — addDestination ─────────────────────────────────────

    @Test
    void addDestination_evictsCache_subsequentListCallHitsRepository() {
        DestinationId id = new DestinationId("United Kingdom", "London");
        Destination dest = new Destination(id, DATE_FROM, DATE_TO);
        when(destinationRepository.findByDateRangeOverlap(any(), any())).thenReturn(List.of());
        when(destinationRepository.existsById(any())).thenReturn(false);
        when(destinationRepository.save(any())).thenReturn(dest);

        destinationService.listDestinations(listRequest(DATE_FROM, DATE_TO));        // miss  → repo call 1
        destinationService.listDestinations(listRequest(DATE_FROM, DATE_TO));        // hit   → no repo call
        destinationService.addDestination(addRequest("United Kingdom", "London"));   // evict
        destinationService.listDestinations(listRequest(DATE_FROM, DATE_TO));        // miss  → repo call 2

        verify(destinationRepository, times(2)).findByDateRangeOverlap(DATE_FROM, DATE_TO);
    }

    // ── @CacheEvict — removeDestination ──────────────────────────────────

    @Test
    void removeDestination_evictsCache_subsequentListCallHitsRepository() {
        DestinationId id = new DestinationId("United Kingdom", "London");
        Destination dest = new Destination(id, DATE_FROM, DATE_TO);
        when(destinationRepository.findByDateRangeOverlap(any(), any())).thenReturn(List.of());
        when(destinationRepository.findById(id)).thenReturn(Optional.of(dest));

        destinationService.listDestinations(listRequest(DATE_FROM, DATE_TO));                    // miss  → repo call 1
        destinationService.listDestinations(listRequest(DATE_FROM, DATE_TO));                    // hit   → no repo call
        destinationService.removeDestination(new RemoveDestinationRequest("United Kingdom", "London")); // evict
        destinationService.listDestinations(listRequest(DATE_FROM, DATE_TO));                    // miss  → repo call 2

        verify(destinationRepository, times(2)).findByDateRangeOverlap(DATE_FROM, DATE_TO);
    }

    @Test
    void removeDestination_evictsAllKeys_otherRangesAlsoRequery() {
        when(destinationRepository.findByDateRangeOverlap(any(), any())).thenReturn(List.of());
        DestinationId id = new DestinationId("United Kingdom", "London");
        when(destinationRepository.findById(id))
                .thenReturn(Optional.of(new Destination(id, DATE_FROM, DATE_TO)));

        LocalDate otherFrom = DATE_FROM.plusMonths(2);
        LocalDate otherTo   = DATE_TO.plusMonths(2);

        destinationService.listDestinations(listRequest(DATE_FROM, DATE_TO));     // cache key A
        destinationService.listDestinations(listRequest(otherFrom, otherTo));     // cache key B
        destinationService.removeDestination(new RemoveDestinationRequest("United Kingdom", "London")); // evict all
        destinationService.listDestinations(listRequest(DATE_FROM, DATE_TO));     // key A re-queried
        destinationService.listDestinations(listRequest(otherFrom, otherTo));     // key B re-queried

        // Both keys should have been queried twice (once before eviction, once after)
        verify(destinationRepository, times(2)).findByDateRangeOverlap(DATE_FROM, DATE_TO);
        verify(destinationRepository, times(2)).findByDateRangeOverlap(otherFrom, otherTo);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private DestinationListRequest listRequest(LocalDate from, LocalDate to) {
        DestinationListRequest req = new DestinationListRequest();
        req.setDateFrom(from);
        req.setDateTo(to);
        return req;
    }

    private DestinationRequest addRequest(String country, String city) {
        DestinationRequest req = new DestinationRequest();
        req.setCountryName(country);
        req.setCityName(city);
        req.setDateFrom(DATE_FROM);
        req.setDateTo(DATE_TO);
        return req;
    }
}
