package dev.damaso.market.controllers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import dev.damaso.market.brokerentities.Order;
import dev.damaso.market.brokerentities.Trade;
import dev.damaso.market.brokerrepositories.OrderRepository;
import dev.damaso.market.brokerrepositories.TradeRepository;
import dev.damaso.market.operations.Date;

@RestController
public class Trades {
    @Autowired
    TradeRepository tradeRepository;

    @Autowired
    OrderRepository orderRepository;

    @GetMapping("/orders/{orderId}/trades")
    Iterable<TradeDTO> getOrderTrades(@PathVariable Integer orderId) {
        Optional<Order> optionalOrder = orderRepository.findById(orderId);
        if (!optionalOrder.isPresent()) {
            throw new NotFoundException();
        }
        Order order = optionalOrder.get();
        LocalDateTime preOpen = Date.getPreOpen(order.createdAt);
        Iterable<Trade> trades = tradeRepository.findByOrderId(orderId);
        List<TradeDTO> resultTrades = new ArrayList<>();
        for (Trade trade : trades) {
            TradeDTO tradeDTO = new TradeDTO();
            tradeDTO.id = trade.id;
            tradeDTO.orderId = trade.orderId;
            tradeDTO.tradeTime = trade.tradeTime;
            tradeDTO.side = trade.side;
            tradeDTO.size = trade.size;
            tradeDTO.price = trade.price;
            tradeDTO.commission = trade.commission;
            tradeDTO.minuteSincePreOpen = Date.minutesBetween(preOpen, trade.tradeTime);
            resultTrades.add(tradeDTO);
        }
        return resultTrades;
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
