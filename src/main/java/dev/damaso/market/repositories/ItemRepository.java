package dev.damaso.market.repositories;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.entities.Item;
import dev.damaso.market.entities.ItemId;

public interface ItemRepository extends CrudRepository<Item, ItemId> {
    @Cacheable("items")
    Iterable<Item> findAll();
}
