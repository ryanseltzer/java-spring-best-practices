package learn.spring_best_practices.repository;

import learn.spring_best_practices.entity.Destination;
import learn.spring_best_practices.entity.DestinationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DestinationRepository extends JpaRepository<Destination, DestinationId> {
}
