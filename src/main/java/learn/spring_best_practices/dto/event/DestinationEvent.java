package learn.spring_best_practices.dto.event;

import learn.spring_best_practices.dto.response.DestinationResponse;

import java.time.Instant;
import java.time.LocalDate;

public record DestinationEvent(
        String countryName,
        String cityName,
        LocalDate dateFrom,
        LocalDate dateTo,
        EventType eventType,
        Instant occurredAt
) {
    public static DestinationEvent from(DestinationResponse response, EventType eventType) {
        return new DestinationEvent(
                response.countryName(),
                response.cityName(),
                response.dateFrom(),
                response.dateTo(),
                eventType,
                Instant.now()
        );
    }
}
