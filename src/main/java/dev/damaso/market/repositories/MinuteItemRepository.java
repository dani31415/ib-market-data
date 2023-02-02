package dev.damaso.market.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.entities.LastItem;
import dev.damaso.market.entities.MinuteItem;
import dev.damaso.market.entities.MinuteItemId;

public interface MinuteItemRepository extends CrudRepository<MinuteItem, MinuteItemId> {
    // Exclude snapshot data since it is incomplete
    @Query(nativeQuery = true, value = "SELECT s.id AS symbolId, max(i.date) AS date FROM symbol s LEFT JOIN minute_item i ON i.symbol_id=s.id and i.source!=2 WHERE s.ib_conid is not null group BY s.id")
    List<LastItem> findMaxDateGroupBySymbol();

    @Query("SELECT mi FROM MinuteItem mi WHERE symbolId=?1 ORDER BY date ASC, minute ASC")
    List<MinuteItem> findBySymbolId(int symbolId);

    @Query("SELECT mi FROM MinuteItem mi WHERE date=?1 ORDER BY symbolId ASC, minute ASC")
    Iterable<MinuteItem> findByDate(LocalDate date);
}
