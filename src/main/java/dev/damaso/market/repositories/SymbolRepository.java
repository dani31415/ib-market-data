package dev.damaso.market.repositories;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.entities.Symbol;

public interface SymbolRepository extends CrudRepository<Symbol, Integer> {
    @Cacheable("symbols")

    @Query("SELECT s FROM Symbol s WHERE s.shortName = ?1 AND s.disabled = false")
    Symbol findSymbolByShortName(String shortName);

    @Query(nativeQuery = true, value = "SELECT s.* FROM symbol s WHERE s.ib_conid IS NOT NULL ORDER BY s.id ASC")
    Iterable<Symbol> findAllIB();
}
