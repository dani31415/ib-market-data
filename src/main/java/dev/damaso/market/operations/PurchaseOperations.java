package dev.damaso.market.operations;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.Vector;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.damaso.market.brokerentities.Log;
import dev.damaso.market.brokerentities.Order;
import dev.damaso.market.brokerrepositories.LogRepository;
import dev.damaso.market.brokerrepositories.OrderRepository;

@Service
public class PurchaseOperations {
    @Autowired
    LogRepository logRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    OrderRepository orderRepository;

    private int computeOrderFromReferrer(String referrer) {
        int i = referrer.indexOf("@");
        String orderIdStr = referrer.substring(1,i);
        return Integer.parseInt(orderIdStr);
    }

    public List<Purchase> getPurchases() throws Exception {
        Iterable<Log> iterableLog = logRepository.findByMessage("POST ib order");
        Map<Integer, Purchase> map = new TreeMap<>();
        for (Log log : iterableLog) {
            String logOrderJson = objectMapper.readValue(log.object, String.class);
            LogOrder logOrder = objectMapper.readValue(logOrderJson, LogOrder.class);
            if (logOrder.side.equals("BUY")) {
                Purchase purchase = new Purchase();
                purchase.quantity = logOrder.quantity;
                purchase.price = logOrder.price;
                purchase.orderType = logOrder.orderType;
                purchase.dateTime = log.createdAt;
                purchase.orderId = computeOrderFromReferrer(logOrder.referrer);
                Optional<Order> optionalOrder = orderRepository.findById(purchase.orderId);
                if (optionalOrder.isPresent()) {
                    Order order = optionalOrder.get();
                    if (order.status.equals("closed") 
                    || order.description !=null && order.description.equals("Order has Cancelled status")) {
                        purchase.buyOrderPrice = order.buyOrderPrice;
                        purchase.buyAt = order.buyAt;
                        purchase.symbolSrcName = order.symbolSrcName;
                        purchase.symbolId = order.symbolId;
                        map.put(purchase.orderId, purchase);
                    }
                }
            }
        }
        List<Purchase> result = new Vector<>();
        for (int key : map.keySet()) {
            result.add(map.get(key));
        }
        System.out.println(result.size());
        return result;
    }
}
