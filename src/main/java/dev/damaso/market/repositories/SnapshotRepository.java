package dev.damaso.market.repositories;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.entities.Snapshot;
import dev.damaso.market.entities.SnapshotId;
import dev.damaso.market.entities.SnapshotWithMinute;

public interface SnapshotRepository extends CrudRepository<Snapshot, SnapshotId> {
    @Query(nativeQuery = true, value = """
        SELECT 
            s.symbol_id as symbolId,
            s.volume,
            s.last,
            s.date,
            s.datetime,
            FLOOR( (60*hour(s.datetime)+minute(datetime) - 1)/10 ) - 78 as minute
        FROM snapshot as s
        WHERE date=?1 order by symbol_id, minute, datetime
    """)
    Iterable<SnapshotWithMinute> findByDate(LocalDate date);

    @Query(nativeQuery = true, value = """
        SELECT 
            s.symbol_id,
            s.volume,
            s.last,
            s.date,
            s.status,
            s.created_at,
            GREATEST(s.datetime, IFNULL(s.created_at,0)) as datetime
        FROM snapshot as s
        WHERE date=?1 order by symbol_id, datetime
    """)
    Iterable<Snapshot> findByDateDeterministic(LocalDate date);

    @Query(nativeQuery = true, value = """
        SELECT 
            s.symbol_id,
            s.volume,
            s.last,
            s.date,
            s.status,
            s.created_at,
            GREATEST(s.datetime, IFNULL(s.created_at,0)) as datetime
        FROM snapshot as s
        WHERE date=?1 and symbol_id=?2 AND created_at<=?3 ORDER BY datetime DESC
    """)
    Iterable<Snapshot> findByDatAndSymbolId(LocalDate date, int symbolId, LocalDateTime createdAt);
}
