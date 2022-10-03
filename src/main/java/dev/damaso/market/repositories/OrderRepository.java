package dev.damaso.market.repositories;

import dev.damaso.market.entities.Order;
import dev.damaso.market.entities.OrderWithSymbol;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface OrderRepository extends CrudRepository<Order, Integer> {
    Optional<Order> findByGroupGuidAndSymbolId(String groupGuid, int symbolId);

    @Query("SELECT o FROM OrderWithSymbol o WHERE o.status = ?1 ORDER BY o.order ASC")
    Iterable<OrderWithSymbol> findAllByStatus(String status);
}
