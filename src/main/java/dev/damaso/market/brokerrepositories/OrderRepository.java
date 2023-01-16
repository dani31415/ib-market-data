package dev.damaso.market.brokerrepositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.brokerentities.Order;

public interface OrderRepository extends CrudRepository<Order, Integer> {
    Optional<Order> findByGroupGuidAndSymbolId(String groupGuid, int symbolId);

    @Query("SELECT o FROM Order o WHERE o.status = ?1 ORDER BY o.order ASC")
    Iterable<Order> findAllByStatus(String status);
}
