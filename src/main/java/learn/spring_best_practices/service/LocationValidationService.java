package learn.spring_best_practices.service;

public interface LocationValidationService {

    /**
     * Validates that the given country and city represent a legitimate location.
     * Throws {@link learn.spring_best_practices.exception.AppException} on failure.
     */
    void validateLocation(String countryName, String cityName);
}
