package learn.spring_best_practices.repository;

import learn.spring_best_practices.entity.Destination;
import learn.spring_best_practices.entity.DestinationId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that run against the auto-configured in-memory H2 database.
 * Each test is wrapped in a transaction that rolls back on completion,
 * ensuring full isolation between parallel test runs.
 */
@SpringBootTest
@Transactional
class DestinationRepositoryTest {

    @Autowired
    DestinationRepository repository;

    private static final String COUNTRY  = "United Kingdom";
    private static final String CITY     = "London";
    private static final LocalDate FROM  = LocalDate.of(2026, 6, 1);
    private static final LocalDate TO    = LocalDate.of(2026, 12, 1);

    // ── Save / Find ───────────────────────────────────────────────────────

    @Test
    void save_persistsDestinationWithCorrectFields() {
        Destination saved = repository.save(destination(COUNTRY, CITY, FROM, TO));

        assertThat(saved.getId().getCountryName()).isEqualTo(COUNTRY);
        assertThat(saved.getId().getCityName()).isEqualTo(CITY);
        assertThat(saved.getDateFrom()).isEqualTo(FROM);
        assertThat(saved.getDateTo()).isEqualTo(TO);
    }

    @Test
    void findById_whenExists_returnsDestination() {
        repository.save(destination(COUNTRY, CITY, FROM, TO));

        Optional<Destination> found = repository.findById(new DestinationId(COUNTRY, CITY));

        assertThat(found).isPresent();
        assertThat(found.get().getDateFrom()).isEqualTo(FROM);
        assertThat(found.get().getDateTo()).isEqualTo(TO);
    }

    @Test
    void findById_whenNotExists_returnsEmpty() {
        Optional<Destination> found = repository.findById(new DestinationId("France", "Paris"));

        assertThat(found).isEmpty();
    }

    // ── existsById ────────────────────────────────────────────────────────

    @Test
    void existsById_whenSaved_returnsTrue() {
        repository.save(destination(COUNTRY, CITY, FROM, TO));

        assertThat(repository.existsById(new DestinationId(COUNTRY, CITY))).isTrue();
    }

    @Test
    void existsById_whenNotSaved_returnsFalse() {
        assertThat(repository.existsById(new DestinationId("Germany", "Berlin"))).isFalse();
    }

    // ── findAll ───────────────────────────────────────────────────────────

    @Test
    void findAll_returnsAllSavedDestinations() {
        repository.save(destination(COUNTRY, CITY, FROM, TO));
        repository.save(destination("France", "Paris", FROM, TO));

        assertThat(repository.findAll()).hasSizeGreaterThanOrEqualTo(2);
    }

    // ── Delete ────────────────────────────────────────────────────────────

    @Test
    void deleteById_removesDestination() {
        repository.save(destination(COUNTRY, CITY, FROM, TO));
        DestinationId id = new DestinationId(COUNTRY, CITY);

        repository.deleteById(id);

        assertThat(repository.existsById(id)).isFalse();
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private Destination destination(String country, String city,
                                    LocalDate from, LocalDate to) {
        return new Destination(new DestinationId(country, city), from, to);
    }
}
