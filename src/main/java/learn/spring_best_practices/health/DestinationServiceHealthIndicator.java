package learn.spring_best_practices.health;

import learn.spring_best_practices.repository.DestinationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Exposes a dedicated health component for the destination service.
 * Performs a live count query so the indicator reflects real repository
 * availability rather than just whether the bean wired up correctly.
 *
 * Visible at: GET /actuator/health/destinationService
 */
@Component
@RequiredArgsConstructor
public class DestinationServiceHealthIndicator implements HealthIndicator {

    private final DestinationRepository destinationRepository;

    @Override
    public Health health() {
        try {
            long count = destinationRepository.count();
            return Health.up()
                    .withDetail("destinationCount", count)
                    .build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
