package learn.spring_best_practices.dto.response;

import learn.spring_best_practices.entity.Destination;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record DestinationResponse(
        String countryName,
        String cityName,
        LocalDate dateFrom,
        LocalDate dateTo
) {
    /**
     * Factory method that maps a persisted {@link Destination} entity to this response.
     * Centralises the entity-to-DTO translation so service methods do not duplicate the builder chain.
     */
    public static DestinationResponse from(Destination destination) {
        return DestinationResponse.builder()
                .countryName(destination.getId().getCountryName())
                .cityName(destination.getId().getCityName())
                .dateFrom(destination.getDateFrom())
                .dateTo(destination.getDateTo())
                .build();
    }
}
