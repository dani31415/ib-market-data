package dev.damaso.market.repositories;

import java.time.LocalDate;
import java.util.Collection;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.entities.Item;
import dev.damaso.market.entities.ItemId;
import dev.damaso.market.entities.LastItem;

public interface ItemRepository extends CrudRepository<Item, ItemId> {
    @Cacheable("items")
    Iterable<Item> findAll();

    // Exclude snapshot data since it is incomplete
    @Query(nativeQuery = true, value = "SELECT max(i.date) AS date, i.symbol_id as symbolId FROM item i WHERE i.source!=2 GROUP BY i.symbol_id")
    Collection<LastItem> findMaxDateGroupBySymbol();

    @Query(nativeQuery = true, value = "SELECT i.* FROM item i INNER JOIN symbol s ON i.symbol_id=s.id WHERE s.ib_conid IS NOT NULL ORDER BY date ASC, symbol_id ASC")
    Iterable<Item> findAllIB();

    @Query("SELECT i FROM Item i INNER JOIN Symbol s ON i.symbolId=s.id WHERE s.ib_conid IS NOT NULL and i.date >= ?1 ORDER BY i.date ASC, i.symbolId ASC")
    Iterable<Item> findAllIBFromDate(LocalDate from);

    @Query("SELECT DISTINCT i.date FROM Item i ORDER BY date ASC")
    Iterable<LocalDate> findAllDates();

    @Query("SELECT DISTINCT i.date FROM Item i WHERE i.date >= ?1 ORDER BY date ASC")
    Iterable<LocalDate> findAllDatesFromDate(LocalDate from);

}
