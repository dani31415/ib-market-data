package dev.damaso.market.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.entities.LastItem;
import dev.damaso.market.entities.MinuteItem;
import dev.damaso.market.entities.MinuteItemBase;
import dev.damaso.market.entities.MinuteItemId;

public interface MinuteItemRepository extends CrudRepository<MinuteItem, MinuteItemId> {
    // Exclude snapshot data since it is incomplete
    @Query(nativeQuery = true, value = "SELECT s.id AS symbolId, max(i.date) AS date FROM symbol s LEFT JOIN minute_item i ON i.symbol_id=s.id and i.source!=2 WHERE s.ib_conid is not null group BY s.id")
    List<LastItem> findMaxDateGroupBySymbol();

    @Query("SELECT mi FROM MinuteItem mi WHERE symbolId=?1 ORDER BY date ASC, minute ASC")
    List<MinuteItem> findBySymbolId(int symbolId);

    @Query("SELECT mi FROM MinuteItem mi WHERE date=?1 ORDER BY symbolId ASC, minute ASC")
    Iterable<MinuteItem> findByDate(LocalDate date);

    @Query("SELECT mi FROM MinuteItem mi WHERE symbolId=?1 AND date=?2 ORDER BY minute ASC")
    Iterable<MinuteItem> findBySymbolIdAndDate(int symbolId, LocalDate date);

    @Query(nativeQuery = true, value = """
    SELECT ot.open AS open, ct.close AS close, h AS high, l AS low, T.minute_group as minute, T.symbol_id as symbolId FROM (
        SELECT MIN(minute) AS o, MAX(minute) AS c, MAX(high) as h, MIN(low) as l, ?2*FLOOR(minute/?2) AS minute_group, symbol_id, date 
        FROM market.minute_item 
        WHERE date=?1
        GROUP BY symbol_id, minute_group, date
    ) as T 
        inner join market.minute_item as ot on ot.symbol_id = T.symbol_id and ot.date=T.date and ot.minute=T.o
        inner join market.minute_item as ct on ct.symbol_id = T.symbol_id and ct.date=T.date and ct.minute=T.c
    """)
    Iterable<MinuteItemBase> findByDateGroupByMinute(LocalDate date, int minute);
}
