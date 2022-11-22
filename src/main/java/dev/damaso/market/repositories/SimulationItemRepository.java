package dev.damaso.market.repositories;
import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.entities.SimulationItem;

public interface SimulationItemRepository extends CrudRepository<SimulationItem, Integer> {
}
