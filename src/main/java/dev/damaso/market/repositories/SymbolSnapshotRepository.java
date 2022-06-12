package dev.damaso.market.repositories;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.entities.Symbol;
import dev.damaso.market.entities.SymbolSnapshot;
import dev.damaso.market.entities.SymbolSnapshotId;

public interface SymbolSnapshotRepository extends CrudRepository<SymbolSnapshot, SymbolSnapshotId> {
    @Query(nativeQuery = true, value = "SELECT max(ss.update_id) FROM symbol_snapshot ss")
    public Date findLastDate();

    @Query("SELECT ss FROM SymbolSnapshot ss WHERE ss.updateId = ?1 ORDER BY ss.symbolId ASC ")
    public List<SymbolSnapshot> findByUpdateId(Date updateId);

    @Query("SELECT s FROM Symbol s INNER JOIN SymbolSnapshot ss ON s.id = ss.symbolId WHERE ss.updateId = ?1 ORDER BY s.id ASC ")
    public List<Symbol> findSymbolsByUpdateId(Date updateId);
}
