package dev.damaso.market.brokerrepositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.brokerentities.SimulationItem;

public interface SimulationItemRepository extends CrudRepository<SimulationItem, Integer> {
    public Optional<SimulationItem> findBySimulationNameAndPeriodAndMinuteAndOrderAndModelName(String name, int period, int minute, int order, String modelName);

    @Query("SELECT o FROM SimulationItem o WHERE o.modelName = ?1 ORDER BY o.id, o.order ASC")
    public Iterable<SimulationItem> findAllByModelName(String modelName);
}
