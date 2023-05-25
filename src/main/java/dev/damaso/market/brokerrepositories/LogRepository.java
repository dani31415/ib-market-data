package dev.damaso.market.brokerrepositories;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import dev.damaso.market.brokerentities.Log;

public interface LogRepository extends PagingAndSortingRepository<Log, Integer> {
    Iterable<Log> findByMessage(String message);

    @Query("""
        SELECT l FROM Log AS l
           WHERE l.createdAt >= ?1 AND l.createdAt < ?2 AND l.object LIKE ?3 ORDER BY l.createdAt ASC
    """)
    Iterable<Log> findByDateRangeAndSubstring(LocalDateTime from, LocalDateTime to, String message);
}
