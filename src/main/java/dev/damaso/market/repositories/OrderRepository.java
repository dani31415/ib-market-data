package dev.damaso.market.repositories;

import dev.damaso.market.entities.Order;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface OrderRepository extends CrudRepository<Order, Integer> {
    Optional<Order> findByGroupGuidAndSymbolId(String groupGuid, int symbolId);

    @Query("SELECT o FROM Order o WHERE o.status = ?1 ORDER BY order ASC")
    Iterable<Order> findAllByStatus(String status);
}
