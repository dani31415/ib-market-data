package dev.damaso.market.brokerrepositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.brokerentities.Trade;

public interface TradeRepository extends CrudRepository<Trade, String> {
    Iterable<Trade> findByOrderId(int orderId);

    @Query(nativeQuery = true, value = "SELECT SUM(t.size * IF(t.side='S',-1,1)) FROM trade AS t WHERE t.ib_order_ref = ?1")
    Double sizeByOrderRef(String ibOrderRef);
}
