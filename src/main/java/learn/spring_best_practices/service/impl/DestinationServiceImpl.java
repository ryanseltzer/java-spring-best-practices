package learn.spring_best_practices.service.impl;

import learn.spring_best_practices.dto.request.DestinationRequest;
import learn.spring_best_practices.dto.response.DestinationResponse;
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
    public DestinationResponse addDestination(DestinationRequest request) {
        // Validate date range — same-day trips are valid, only reject dateFrom > dateTo
        if (request.getDateFrom().isAfter(request.getDateTo())) {
            throw new AppException(AppErrorCode.INVALID_DATE_RANGE);
        }

        // Validate country and city against ISO 3166-1 data
        locationValidationService.validateLocation(request.getCountryName(), request.getCityName());

        // A01: explicit duplicate check before insert — not relying solely on DB constraint
        DestinationId id = new DestinationId(
                request.getCountryName().trim(),
                request.getCityName().trim()
        );
        if (destinationRepository.existsById(id)) {
            throw new AppException(AppErrorCode.DUPLICATE_DESTINATION);
        }

        Destination saved = destinationRepository.save(
                new Destination(id, request.getDateFrom(), request.getDateTo())
        );

        // A09: log success without PII or sensitive payload content
        log.info("Destination added [country={}, city={}]",
                saved.getId().getCountryName(), saved.getId().getCityName());

        return DestinationResponse.builder()
                .countryName(saved.getId().getCountryName())
                .cityName(saved.getId().getCityName())
                .dateFrom(saved.getDateFrom())
                .dateTo(saved.getDateTo())
                .build();
    }

    @Override
    @Transactional
    public DestinationResponse removeDestination(DestinationId id) {
        Destination deleteDestination = destinationRepository.findById(id).orElseThrow(() -> new AppException(AppErrorCode.DESTINATION_NOT_FOUND));
        locationValidationService.validateLocation(id.getCountryName(), id.getCityName());
        
        destinationRepository.delete(deleteDestination);
        return DestinationResponse.builder()
                .countryName(deleteDestination.getId().getCountryName())
                .cityName(deleteDestination.getId().getCityName())
                .dateFrom(deleteDestination.getDateFrom())
                .dateTo(deleteDestination.getDateTo())
                .build();

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
}
