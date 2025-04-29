package dev.damaso.market.repositories;

import org.springframework.data.repository.CrudRepository;
import dev.damaso.market.entities.MinuteItemUpdate;
import dev.damaso.market.entities.MinuteItemUpdateId;

public interface MinuteItemUpdateRepository extends CrudRepository<MinuteItemUpdate, MinuteItemUpdateId> {
}
