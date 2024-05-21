package dev.damaso.market.brokerrepositories;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import dev.damaso.market.brokerentities.Log;

public interface LogRepository extends PagingAndSortingRepository<Log, Integer> {
    Iterable<Log> findByMessage(String message);

    @Query("""
        SELECT l FROM Log AS l
           WHERE l.createdAt >= ?1 AND l.createdAt < ?2 AND l.object LIKE ?3 AND l.source=?4 ORDER BY l.createdAt ASC, l.id ASC
    """)
    Iterable<Log> findByDateRangeAndSubstringAndSource(LocalDateTime from, LocalDateTime to, String message, String source);

    @Query("""
        SELECT l FROM Log AS l
           WHERE l.createdAt >= ?1 AND l.createdAt < ?2 AND (l.object LIKE ?3 or l.object LIKE ?4) AND (l.source=?5 OR l.source=?6) ORDER BY l.createdAt ASC, l.id ASC
    """)
    Iterable<Log> findByDateRangeAndSubstringAndSource2(LocalDateTime from, LocalDateTime to, String pattern0, String pattern1, String source0, String source1);

    @Query("""
        SELECT l FROM Log AS l
           WHERE l.createdAt >= ?1 AND l.createdAt < ?2 AND l.object LIKE ?3 ORDER BY l.createdAt ASC, l.id ASC
    """)
    Iterable<Log> findByDateRangeAndSubstring(LocalDateTime from, LocalDateTime to, String message);


    @Query("""
        SELECT l FROM Log AS l
            WHERE l.createdAt >= ?1 
                AND l.createdAt < ?2 
                AND (l.object LIKE ?3 
                    OR l.message='Http PATCH' AND l.object LIKE ?4 AND l.createdAt BETWEEN '2023-06-15' AND '2023-06-19')
            ORDER BY l.createdAt ASC
    """)
    Iterable<Log> findByDateRangeAndSubstring2(LocalDateTime from, LocalDateTime to, String message1, String message2);

}
