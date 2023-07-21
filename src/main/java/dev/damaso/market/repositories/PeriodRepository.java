package dev.damaso.market.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.entities.Period;

public interface PeriodRepository extends CrudRepository<Period, Integer> {
    Optional<Period> findByDate(LocalDate date);

    List<Period> findByUpdated(boolean updated);

    @Query("""
        SELECT avg(i2.open/i1.open) AS mean FROM Item AS i1
	       INNER JOIN Period AS p1 ON i1.date=p1.date
           INNER JOIN Period AS p2 ON p1.id=p2.id-1
           INNER JOIN Item AS i2 ON i2.date=p2.date AND i1.symbolId = i2.symbolId
           WHERE i2.date=?1
            AND i2.open/i1.open between 0.8 and 1.9 
            AND i1.open*i1.volume>50000
           GROUP BY p2.id, p2.date
    """)
    Optional<Double> computeMeanByDate(LocalDate date);

    @Query("SELECT max(p.date) FROM Period AS p WHERE p.date != ?1")
    LocalDate getLastExcept(LocalDate date);
}

