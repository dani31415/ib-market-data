package dev.damaso.market.brokerrepositories;

import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.brokerentities.Trade;

public interface TradeRepository extends CrudRepository<Trade, String> {
    Iterable<Trade> findByOrderId(int orderId);
}
