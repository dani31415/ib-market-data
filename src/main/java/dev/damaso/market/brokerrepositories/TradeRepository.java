package dev.damaso.market.brokerrepositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.brokerentities.Trade;
import dev.damaso.market.brokerentities.TradeSideEnum;

public interface TradeRepository extends CrudRepository<Trade, String> {
    Iterable<Trade> findByOrderId(int orderId);

    @Query(nativeQuery = true, value = "SELECT SUM(t.size * IF(t.side='S',-1,1)) FROM trade AS t WHERE t.ib_order_ref = ?1")
    Double sizeByOrderRef(String ibOrderRef);

    @Query("SELECT SUM(t.size) FROM Trade t WHERE t.orderId = ?1 AND t.side = ?2")
    Optional<Double> sumSizeBySide(int orderId, TradeSideEnum side);
}
