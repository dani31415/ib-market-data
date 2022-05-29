package dev.damaso.market.repositories;

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

    @Query(nativeQuery = true, value = "SELECT max(i.date) AS date, i.symbol_id as symbolId FROM item i GROUP BY i.symbol_id")
    Collection<LastItem> findMaxDateGroupBySymbol();
}
