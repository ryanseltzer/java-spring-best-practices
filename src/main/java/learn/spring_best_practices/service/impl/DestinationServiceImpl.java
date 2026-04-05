package learn.spring_best_practices.service.impl;

import learn.spring_best_practices.dto.request.DestinationListRequest;
import learn.spring_best_practices.dto.request.DestinationRequest;
import learn.spring_best_practices.dto.request.RemoveDestinationRequest;
import learn.spring_best_practices.dto.response.DestinationListResponse;
import learn.spring_best_practices.dto.response.DestinationResponse;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import learn.spring_best_practices.entity.Destination;
import learn.spring_best_practices.entity.DestinationId;
import learn.spring_best_practices.exception.AppErrorCode;
import learn.spring_best_practices.exception.AppException;
import learn.spring_best_practices.repository.DestinationRepository;
import learn.spring_best_practices.service.DestinationService;
import learn.spring_best_practices.service.LocationValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DestinationServiceImpl implements DestinationService {

    private final DestinationRepository destinationRepository;
    private final LocationValidationService locationValidationService;

    @Override
    @Transactional
    @CacheEvict(cacheNames = "destinations", allEntries = true)
    public DestinationResponse addDestination(DestinationRequest request) {
        validateDateRange(request.getDateFrom(), request.getDateTo());

        // Validate country and city against ISO 3166-1 data
        locationValidationService.validateLocation(request.getCountryName(), request.getCityName());

        // A01: explicit duplicate check before insert — not relying solely on DB constraint
        DestinationId id = buildId(request);
        if (destinationRepository.existsById(id)) {
            throw new AppException(AppErrorCode.DUPLICATE_DESTINATION);
        }

        Destination saved = destinationRepository.save(
                new Destination(id, request.getDateFrom(), request.getDateTo())
        );

        // A09: log success without PII or sensitive payload content
        log.info("Destination added [country={}, city={}]",
                saved.getId().getCountryName(), saved.getId().getCityName());

        return DestinationResponse.from(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "destinations", allEntries = true)
    public DestinationResponse removeDestination(RemoveDestinationRequest request) {
        DestinationId id = new DestinationId(request.countryName(), request.cityName());
        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new AppException(AppErrorCode.DESTINATION_NOT_FOUND));
        destinationRepository.delete(destination);
        return DestinationResponse.from(destination);
    }

    @Override
    public DestinationResponse verifyDestination(DestinationRequest request) {
        // Validate country and city against ISO 3166-1 data
        locationValidationService.validateLocation(request.getCountryName(), request.getCityName());

        return DestinationResponse.builder()
                .countryName(request.getCountryName())
                .cityName(request.getCityName())
                .dateFrom(request.getDateFrom())
                .dateTo(request.getDateTo())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "destinations", key = "#request.dateFrom + '-' + #request.dateTo")
    public DestinationListResponse listDestinations(DestinationListRequest request) {
        validateDateRange(request.getDateFrom(), request.getDateTo());

        if (ChronoUnit.DAYS.between(request.getDateFrom(), request.getDateTo()) > 366) {
            throw new AppException(AppErrorCode.DATE_SPAN_TOO_LARGE);
        }

        List<DestinationResponse> results = destinationRepository
                .findByDateRangeOverlap(request.getDateFrom(), request.getDateTo())
                .stream()
                .map(DestinationResponse::from)
                .toList();

        log.debug("listDestinations returned {} result(s)", results.size());
        return DestinationListResponse.of(results);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "destinations", allEntries = true)
    public DestinationResponse updateDestination(DestinationRequest request) {
        validateDateRange(request.getDateFrom(), request.getDateTo());

        locationValidationService.validateLocation(request.getCountryName(), request.getCityName());

        DestinationId id = buildId(request);
        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new AppException(AppErrorCode.DESTINATION_NOT_FOUND));

        destination.setDateFrom(request.getDateFrom());
        destination.setDateTo(request.getDateTo());

        Destination saved = destinationRepository.save(destination);

        log.info("Destination updated [country={}, city={}]",
                saved.getId().getCountryName(), saved.getId().getCityName());

        return DestinationResponse.from(saved);
    }

    // ── Shared validation ─────────────────────────────────────────────────

    /** Same-day trips (dateFrom == dateTo) are valid; only reject dateFrom > dateTo. */
    private void validateDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom.isAfter(dateTo)) {
            throw new AppException(AppErrorCode.INVALID_DATE_RANGE);
        }
    }

    private DestinationId buildId(DestinationRequest request) {
        return new DestinationId(request.getCountryName().trim(), request.getCityName().trim());
    }
}
