package dev.damaso.market.repositories;

import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.entities.MinuteItem;
import dev.damaso.market.entities.MinuteItemId;

public interface MinuteItemRepository extends CrudRepository<MinuteItem, MinuteItemId> {
}
