package dev.damaso.market.repositories;

import java.time.LocalDate;
import java.util.Optional;

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

    @Query(nativeQuery = true, value = "SELECT s.* FROM symbol s WHERE s.ib_conid IS NOT NULL AND not s.disabled ORDER BY s.id ASC")
    Iterable<Symbol> findAllEnabled();

    @Query(nativeQuery = true, value = "SELECT s.* FROM symbol s WHERE s.ib_conid = ?1")
    Optional<Symbol> findByIb_conid(String ib_conid);

   @Query("""
        SELECT s
            FROM Symbol s 
            INNER JOIN Item i
            ON s.id = i.symbolId AND i.version = 0 AND i.date = ?1 AND (i.volume > ?2 or i.open * i.volume > ?2)
            ORDER BY s.id ASC
    """)
    Iterable<Symbol> findAllActive(LocalDate date, long volume);
}
