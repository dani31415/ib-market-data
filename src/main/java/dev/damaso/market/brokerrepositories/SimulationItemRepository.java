package dev.damaso.market.brokerrepositories;
import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.brokerentities.SimulationItem;

public interface SimulationItemRepository extends CrudRepository<SimulationItem, Integer> {
}
