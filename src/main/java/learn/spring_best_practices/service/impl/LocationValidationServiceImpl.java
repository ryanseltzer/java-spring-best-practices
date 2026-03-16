package learn.spring_best_practices.service.impl;

import learn.spring_best_practices.exception.AppErrorCode;
import learn.spring_best_practices.exception.AppException;
import learn.spring_best_practices.service.LocationValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LocationValidationServiceImpl implements LocationValidationService {

    // Derived once at startup from JDK ISO 3166-1 data — no hardcoding required
    private static final Set<String> VALID_COUNTRIES = Arrays.stream(Locale.getISOCountries())
            .map(code -> Locale.of("", code).getDisplayCountry(Locale.ENGLISH).toUpperCase())
            .collect(Collectors.toUnmodifiableSet());

    @Override
    public void validateLocation(String countryName, String cityName) {
        // A03: inputs are already @Pattern-validated by the DTO before this is called

        if (!VALID_COUNTRIES.contains(countryName.trim().toUpperCase())) {
            // A09: do not echo raw user input into the log (enumeration / log-injection risk)
            log.warn("Location validation failed [{}]: unrecognised country submitted",
                    AppErrorCode.INVALID_COUNTRY.getAppCode());
            throw new AppException(AppErrorCode.INVALID_COUNTRY);
        }

        if (cityName.trim().length() < 2) {
            log.warn("Location validation failed [{}]: city name too short",
                    AppErrorCode.INVALID_CITY.getAppCode());
            throw new AppException(AppErrorCode.INVALID_CITY);
        }

        // Note: full city-existence verification would require an external geographic API
        log.debug("Location validation passed");
    }
}
