package learn.spring_best_practices.health;

import learn.spring_best_practices.repository.DestinationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DestinationServiceHealthIndicatorTest {

    @Mock
    DestinationRepository destinationRepository;

    @InjectMocks
    DestinationServiceHealthIndicator healthIndicator;

    @Test
    void health_repositoryReachable_returnsUpWithCount() {
        when(destinationRepository.count()).thenReturn(42L);

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("destinationCount", 42L);
    }

    @Test
    void health_repositoryThrows_returnsDown() {
        RuntimeException cause = new RuntimeException("DB unavailable");
        when(destinationRepository.count()).thenThrow(cause);

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }
}
