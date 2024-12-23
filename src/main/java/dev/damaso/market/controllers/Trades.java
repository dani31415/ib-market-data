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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.damaso.market.brokerentities.Order;
import dev.damaso.market.brokerentities.Trade;
import dev.damaso.market.brokerentities.TradeSideEnum;
import dev.damaso.market.brokerrepositories.OrderRepository;
import dev.damaso.market.brokerrepositories.TradeRepository;
import dev.damaso.market.operations.Date;

@RestController
public class Trades {
    @Autowired
    TradeRepository tradeRepository;

    @Autowired
    OrderRepository orderRepository;

    @GetMapping("/orders/trades/size")
    double getSizeByOrderRef(@RequestParam String ibOrderRef) {
        Double quantity = tradeRepository.sizeByOrderRef(ibOrderRef);
        if (quantity == null) {
            return 0;
        }
        return quantity;
    }

    @GetMapping("/orders/{orderId}/trades/sell")
    double sumSizeTradesSell(@PathVariable int orderId) {
        Optional<Double> sum = tradeRepository.sumSizeBySide(orderId, TradeSideEnum.S);
        if (sum.isPresent()) {
            return sum.get();
        }
        return 0.0;
    }

    @GetMapping("/orders/{orderId}/trades/buy")
    double sumSizeTradesBuy(@PathVariable int orderId) {
        Optional<Double> sum = tradeRepository.sumSizeBySide(orderId, TradeSideEnum.B);
        if (sum.isPresent()) {
            return sum.get();
        }
        return 0.0;
    }

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
            tradeDTO.ibOrderRef = trade.ibOrderRef;
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
        trade.ibOrderRef = createTradeDTO.ibOrderRef;
        trade.tradeTime = createTradeDTO.tradeTime;
        trade.side = createTradeDTO.side;
        trade.size = createTradeDTO.size;
        trade.price = createTradeDTO.price;
        trade.commission = createTradeDTO.commission;
        tradeRepository.save(trade);
    }
}
