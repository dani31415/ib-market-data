package dev.damaso.market.repositories;

import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.entities.ItemId;
import dev.damaso.market.entities.MinuteItem;

public interface MinuteDataRepository extends CrudRepository<MinuteItem, ItemId> {
}
