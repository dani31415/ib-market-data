package dev.damaso.market.brokerrepositories;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.brokerentities.Order;

public interface OrderRepository extends CrudRepository<Order, Integer> {
    Optional<Order> findByGroupGuidAndSymbolId(String groupGuid, int symbolId);

    @Query("SELECT o FROM Order o WHERE o.status = ?1 ORDER BY o.order ASC")
    Iterable<Order> findAllByStatus(String status);

    @Query("SELECT o FROM Order o WHERE o.date = ?1 ORDER BY o.order ASC")
    Iterable<Order> findAllByDate(LocalDate date);

    @Query("SELECT o FROM Order o WHERE o.modelName = ?1 ORDER BY o.id, o.order ASC")
    Iterable<Order> findAllByModelName(String modelName);

    @Query("SELECT o FROM Order o WHERE o.date = ?1 AND o.minute = ?2 ORDER BY o.order ASC")
    Iterable<Order> findAllByDateAndMinute(LocalDate date, int minute);

    @Query("SELECT o FROM Order o WHERE o.status = 'opening' AND o.purchaseExpires IS NOT NULL ORDER BY o.purchaseExpires ASC")
    Iterable<Order> findOpeningSortedByPurchaseExpires();
}
