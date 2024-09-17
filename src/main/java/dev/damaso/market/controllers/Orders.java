package dev.damaso.market.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import dev.damaso.market.brokerentities.Log;
import dev.damaso.market.brokerentities.Order;
import dev.damaso.market.brokerrepositories.LogRepository;
import dev.damaso.market.brokerrepositories.OrderRepository;
import dev.damaso.market.entities.Symbol;
import dev.damaso.market.operations.Date;
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
    LogRepository logRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    PurchaseOperations purchaseOperations;

    @PostMapping("/orders")
    public Order createOrder(@RequestBody OrderRequestDTO orderRequest, @RequestParam(required=false) Boolean unique) throws Exception {
        Symbol symbol = symbolRepository.findSymbolByShortName(orderRequest.symbolSrcName);
        if (symbol==null) {
            throw new Exception("Missing symbol " + orderRequest.symbolSrcName);
        }
        Optional<Order> optionalOrder = orderRepository.findByGroupGuidAndSymbolId(orderRequest.groupGuid, symbol.id);
        if (optionalOrder.isPresent()) {
            // nothing to do
            return optionalOrder.get();
        }
        Order order = new Order();
        order.groupGuid = orderRequest.groupGuid;
        order.order = orderRequest.order;
        order.modelName = orderRequest.modelName;
        order.symbolId = symbol.id;
        order.symbolSrcName = orderRequest.symbolSrcName;
        order.ib_conid = symbol.ib_conid;
        if (orderRequest.date == null) {
            order.date = LocalDate.now(ZoneId.of("America/New_York"));
        } else {
            // Used during testing
            LocalDate localDate = LocalDate.parse(orderRequest.date);
            order.date = localDate;
        }
        order.createdAt = LocalDateTime.now(ZoneId.of("UTC"));
        order.openPrice = orderRequest.openPrice;
        order.status = orderRequest.status;
        order.minute = orderRequest.minute;
        order.buyDesiredPrice = orderRequest.buyDesiredPrice;
        order.sellDesiredPrice = orderRequest.sellDesiredPrice;
        order.optimization = orderRequest.optimization;
        order.purchaseExpires = orderRequest.purchaseExpires;
        order.modelLastPrice = orderRequest.lastPrice;

        if (unique!=null && unique.booleanValue()) {
            Iterable<Order> existingOrders = orderRepository.findAllBySymbolId(symbol.id);
            for (Order existingOrder : existingOrders) {
                if (existingOrder.status.equals("created") ||
                    existingOrder.status.equals("valid") ||
                    existingOrder.status.equals("open") ||
                    existingOrder.status.equals("opening") ||
                    existingOrder.status.equals("closing")
                ) {
                    order.status = "duplicated";
                }
            }
        }

        Order newOrder = orderRepository.save(order);
        return newOrder;
    }

    @GetMapping("/orders")
    @Transactional(transactionManager = "brokerTransactionManager", isolation = Isolation.REPEATABLE_READ)
    public Iterable<Order> getOrders(@RequestParam(required=false) String status, @RequestParam(required=false) String date, @RequestParam(required=false) Integer minute, @RequestParam(required=false) String modelName) throws Exception {
        if (status != null) {
            return orderRepository.findAllByStatus(status);
        } else if (date != null && minute != null) {
            LocalDate localDate = LocalDate.parse(date);
            return orderRepository.findAllByDateAndMinute(localDate, minute);
        } else if (date != null) {
            LocalDate localDate = LocalDate.parse(date);
            return orderRepository.findAllByDate(localDate);
        } else if (modelName != null) {
            return orderRepository.findAllByModelName(modelName);
        } else {
            return orderRepository.findAll();
        }
    }

    @PatchMapping("/orders/{id}")
    @Transactional(transactionManager = "brokerTransactionManager", isolation = Isolation.REPEATABLE_READ)
    public Order patchOrder(@PathVariable Integer id, @RequestBody String inputJson) throws Exception {
        // System.out.println(inputJson);
        Order order = orderRepository.findById(id).orElseThrow(NotFoundException::new);
        ObjectReader objectReader = objectMapper.readerForUpdating(order);
        Order updatedOrder = objectReader.readValue(inputJson);
        updatedOrder.updatedAt = LocalDateTime.now(ZoneId.of("UTC"));
        orderRepository.save(updatedOrder);
        return updatedOrder;
    }

    @GetMapping("/orders/{orderId}/orders")
    public List<IbOrderDTO> findOrdersOrderById(@PathVariable Integer orderId) throws Exception {
        Optional<Order> optionalOrder = orderRepository.findById(orderId);
        if (!optionalOrder.isPresent()) {
            throw new NotFoundException();
        }
        Order order = optionalOrder.get();
        LocalDateTime from = order.createdAt;
        LocalDateTime to;
        if (order.boughtQuantity != null && order.boughtQuantity == order.soldQuantity) {
            to = order.sellAt;
            // Maybe the log is created after a while
            to = to.plus(5, ChronoUnit.MINUTES);
        } else {
            to = LocalDateTime.now();
        }
        LocalDateTime preOpen = Date.getPreOpen(order.createdAt);
        String pattern0 = "%:" + orderId + "%";
        String pattern1 = "%#" + orderId + "%";
        Iterable<Log> logs = logRepository.findByDateRangeAndSubstringAndSource2(from, to, pattern0, pattern1, "EXECUTE_ORDER", "SERVER");
        List<IbOrderDTO> ibOrderList = new ArrayList<>();
        IbOrderDTO previousLogOrder = null;
        for (Log log : logs) {
            String logOrderJson = objectMapper.readValue(log.object, String.class);
            IbOrderDTO logOrder = null;
            if (log.message.equals("Http PATCH")) {
                ChangeLog changeLog = objectMapper.readValue(logOrderJson, ChangeLog.class);
                logOrder = new IbOrderDTO();
                logOrder.side = changeLog.side;
                logOrder.quantity = changeLog.newQuantity;
                logOrder.price = changeLog.newPrice;
            } else if (log.message.equals("Http POST")) {
                logOrder = objectMapper.readValue(logOrderJson, IbOrderDTO.class);
            } else if (log.message.equals("Http DELETE")) {
                logOrder = new IbOrderDTO();
                logOrder.side = "STOP";
            }
            if (logOrder != null) {
                logOrder.createdAt = log.createdAt;
                logOrder.minuteSincePreOpen = Date.minutesBetween(preOpen, log.createdAt);

                // Add only if price, quantity or side change
                boolean ignoreLogOrder = false;
                if (previousLogOrder != null) {
                    if (previousLogOrder.quantity == logOrder.quantity && previousLogOrder.price == logOrder.price && previousLogOrder.side.equals(logOrder.side)) {
                        ignoreLogOrder = true;
                    }
                }
                if (!ignoreLogOrder) {
                    ibOrderList.add(logOrder);
                }
                previousLogOrder = logOrder;
            }
        }
        return ibOrderList;
    }

    @GetMapping("/orders/{orderId}")
    @Transactional(transactionManager = "brokerTransactionManager", isolation = Isolation.REPEATABLE_READ)
    public Optional<Order> findOrderById(@PathVariable Integer orderId) throws Exception {
        return orderRepository.findById(orderId);
    }

    @GetMapping("/purchases")
    public List<Purchase> getPurchases() throws Exception {
        return purchaseOperations.getPurchases();
    }
}
