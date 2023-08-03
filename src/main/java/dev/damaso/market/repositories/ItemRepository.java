package dev.damaso.market.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.entities.Item;
import dev.damaso.market.entities.ItemId;
import dev.damaso.market.entities.LastItem;

public interface ItemRepository extends CrudRepository<Item, ItemId> {
    // Exclude snapshot data since it is incomplete
    @Query(nativeQuery = true, value = "SELECT s.id AS symbolId, max(i.date) AS date FROM symbol s LEFT JOIN item i ON i.symbol_id=s.id and i.source!=2 WHERE s.ib_conid is not null group BY s.id")
    List<LastItem> findMaxDateGroupBySymbol();

    @Query(nativeQuery = true, value = "SELECT symbol_id as symbolId, MAX(date) AS date FROM item WHERE since_pre_open > 0 GROUP BY symbol_id")
    List<LastItem> findLastOpenDates();

    @Query(nativeQuery = true, value = "SELECT MAX(date) AS date FROM item")
    LocalDate findLastDate();

    @Query(nativeQuery = true, value = "SELECT i.* FROM item i INNER JOIN symbol s ON i.symbol_id=s.id WHERE s.ib_conid IS NOT NULL AND i.version=?1 ORDER BY date ASC, symbol_id ASC")
    Iterable<Item> findAllIB(int version);

    @Query("SELECT i FROM Item i INNER JOIN Symbol s ON i.symbolId=s.id WHERE s.ib_conid IS NOT NULL and i.date >= ?1 AND i.version=?2 ORDER BY i.date ASC, i.symbolId ASC")
    Iterable<Item> findAllIBFromDate(LocalDate from, int version);

    @Query("SELECT i FROM Item i INNER JOIN Symbol s ON i.symbolId=s.id WHERE s.ib_conid IS NOT NULL and i.date < ?1 AND i.version=?2 ORDER BY i.date ASC, i.symbolId ASC")
    Iterable<Item> findAllIBToDate(LocalDate from, int version);

    @Query("SELECT DISTINCT i.date FROM Item i ORDER BY date ASC")
    Iterable<LocalDate> findAllDates();

    @Query("SELECT DISTINCT i.date FROM Item i WHERE i.date >= ?1 ORDER BY date ASC")
    Iterable<LocalDate> findAllDatesFromDate(LocalDate from);

    @Query("SELECT DISTINCT i.date FROM Item i WHERE i.date < ?1 ORDER BY date ASC")
    Iterable<LocalDate> findAllDatesToDate(LocalDate to);

    // https://www.baeldung.com/spring-jdbctemplate-in-list
    // Volume average from date and ids
    @Query("SELECT avg(i.volume) FROM Item i WHERE i.date >= ?1 AND i.symbolId = ?2 AND i.volume>0 AND i.version = ?3")
    float findAverageVolume(LocalDate from, int symbol_id, int version);

    @Query("SELECT i FROM Item i WHERE date=?1 ORDER BY symbolId ASC")
    Iterable<Item> findByDate(LocalDate date);

    Iterable<Item> findAllBySymbolIdAndDate(int symbolId, LocalDate date);

    Iterable<Item> findAllBySymbolIdAndDateAndVersion(int symbolId, LocalDate date, int version);

    @Query(nativeQuery = true, value = """
        SELECT I1.symbol_id
            FROM market.item  AS I1
            LEFT JOIN market.item AS I2 ON I1.symbol_id = I2.symbol_id AND I2.date = ?2
            WHERE I1.date >= ?1 AND I1.date < ?2 AND I2.symbol_id IS NULL
            GROUP BY I1.symbol_id           
    """)
    Iterable<Integer> findMissingItems(LocalDate since, LocalDate date);
}
