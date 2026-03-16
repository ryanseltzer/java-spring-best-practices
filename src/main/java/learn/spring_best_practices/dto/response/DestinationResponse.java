package learn.spring_best_practices.dto.response;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record DestinationResponse(
        String countryName,
        String cityName,
        LocalDate dateFrom,
        LocalDate dateTo
) {}
