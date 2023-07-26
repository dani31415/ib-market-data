package dev.damaso.market.repositories;

import java.time.LocalDate;

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
            s.updated_at as updatedAt,
            FLOOR( (60*hour(s.updated_at)+minute(updated_at) - 1)/10 ) - 78 as minute
        FROM snapshot as s
        WHERE date=?1 order by symbol_id, minute, updated_at
    """)
    Iterable<SnapshotWithMinute> findByDate(LocalDate date);
}
