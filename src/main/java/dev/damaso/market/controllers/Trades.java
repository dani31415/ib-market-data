package dev.damaso.market.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import dev.damaso.market.brokerentities.Trade;
import dev.damaso.market.brokerrepositories.TradeRepository;

@RestController
public class Trades {
    @Autowired
    TradeRepository tradeRepository;

    @GetMapping("/orders/{orderId}/trades")
    Iterable<Trade> getOrderTrades(@PathVariable Integer orderId) {
        return tradeRepository.findByOrderId(orderId);
    }

    @PostMapping("/orders/{orderId}/trades")
    void createOrderTrade(@PathVariable Integer orderId, @RequestBody CreateTradeRequestDTO createTradeDTO) {
        Trade trade = new Trade();
        trade.id = createTradeDTO.id;
        trade.orderId = orderId;
        trade.tradeTime = createTradeDTO.tradeTime;
        trade.side = createTradeDTO.side;
        trade.size = createTradeDTO.size;
        trade.price = createTradeDTO.price;
        trade.commission = createTradeDTO.commission;
        tradeRepository.save(trade);
    }
}
