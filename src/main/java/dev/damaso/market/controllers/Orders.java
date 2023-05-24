package dev.damaso.market.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import dev.damaso.market.brokerentities.Order;
import dev.damaso.market.brokerrepositories.OrderRepository;
import dev.damaso.market.entities.Symbol;
import dev.damaso.market.operations.Purchase;
import dev.damaso.market.operations.PurchaseOperations;
import dev.damaso.market.repositories.SymbolRepository;

@RestController
public class Orders {
    @Autowired
    OrderRepository orderRepository;

    @Autowired
    SymbolRepository symbolRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    PurchaseOperations purchaseOperations;

    @PostMapping("/orders")
    public boolean createOrder(@RequestBody OrderRequestDTO orderRequest) throws Exception {
        Symbol symbol = symbolRepository.findSymbolByShortName(orderRequest.symbol);
        if (symbol==null) {
            throw new Exception("Missing symbol " + orderRequest.symbol);
        }
        Optional<Order> optionalOrder = orderRepository.findByGroupGuidAndSymbolId(orderRequest.guid, symbol.id);
        if (optionalOrder.isPresent()) {
            // nothing to do
            return true;
        }
        Order order = new Order();
        order.groupGuid = orderRequest.guid;
        order.order = orderRequest.order;
        order.modelName = orderRequest.modelName;
        order.symbolId = symbol.id;
        order.symbolSrcName = orderRequest.symbol;
        order.ib_conid = symbol.ib_conid;
        order.date = LocalDate.now(ZoneId.of("America/New_York"));
        order.createdAt = LocalDateTime.now(ZoneId.of("UTC"));
        order.openPrice = orderRequest.openPrice;
        order.status = orderRequest.status;
        orderRepository.save(order);
        return true;
    }

    @GetMapping("/orders")
    public Iterable<Order> getOrders(@RequestParam(required=false) String status, @RequestParam(required=false) String date) throws Exception {
        if (status != null) {
            return orderRepository.findAllByStatus(status);
        } else if (date != null) {
            LocalDate localDate = LocalDate.parse(date);
            return orderRepository.findAllByDate(localDate);
        } else {
            return orderRepository.findAll();
        }
    }

    @PatchMapping("/orders/{id}")
    public Order patchOrder(@PathVariable Integer id, @RequestBody String inputJson) throws Exception {
        System.out.println(inputJson);
        Order order = orderRepository.findById(id).orElseThrow(NotFoundException::new);
        ObjectReader objectReader = objectMapper.readerForUpdating(order);
        Order updatedOrder = objectReader.readValue(inputJson);
        updatedOrder.updatedAt = LocalDateTime.now(ZoneId.of("UTC"));
        orderRepository.save(updatedOrder);
        return updatedOrder;
    }

    @GetMapping("/orders/{orderId}")
    public Optional<Order> findOrderById(@PathVariable Integer orderId) throws Exception {
        return orderRepository.findById(orderId);
    }

    @GetMapping("/purchases")
    public List<Purchase> getPurchases() throws Exception {
        return purchaseOperations.getPurchases();
    }
}
