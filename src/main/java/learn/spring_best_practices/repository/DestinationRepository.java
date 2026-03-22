package learn.spring_best_practices.repository;

import learn.spring_best_practices.entity.Destination;
import learn.spring_best_practices.entity.DestinationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DestinationRepository extends JpaRepository<Destination, DestinationId> {

    // Returns destinations whose date range overlaps with [dateFrom, dateTo]
    @Query("SELECT d FROM Destination d WHERE d.dateFrom <= :dateTo AND d.dateTo >= :dateFrom")
    List<Destination> findByDateRangeOverlap(@Param("dateFrom") LocalDate dateFrom,
                                             @Param("dateTo") LocalDate dateTo);
}
