package learn.spring_best_practices.dto.response;

import java.util.List;

public record DestinationListResponse(
        List<DestinationResponse> destinations,
        int count
) {
    public static DestinationListResponse of(List<DestinationResponse> destinations) {
        return new DestinationListResponse(destinations, destinations.size());
    }
}
