package dev.damaso.market.controllers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dev.damaso.market.MarketApplication;
import dev.damaso.market.brokerentities.Log;
import dev.damaso.market.brokerentities.Order;
import dev.damaso.market.brokerrepositories.LogRepository;
import dev.damaso.market.brokerrepositories.OrderRepository;
import dev.damaso.market.controllers.websocket.IbWebSocketClient;
import dev.damaso.market.external.ibgw.Api;
import dev.damaso.market.operations.CloseOrder;
import dev.damaso.market.operations.IbOrderScheduler;

@Component
public class Scheduler {
    public final int RATE = 60*1000;
    public final int WEBSOCKET_RATE = 10*1000;
    public final int ORDER_RATE = 10*1000;

    @Autowired
    CloseOrder closeOrder;

    @Autowired
    LogRepository logRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    IbWebSocketClient ibWebSocketClient;

    @Autowired
    IbOrderScheduler ibOrderScheduler;

    @Autowired
    Api api;


    private void waitToDateTime(ZonedDateTime expires, String message) throws InterruptedException {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        long wait = now.until(expires, ChronoUnit.MILLIS);
        if (wait > 0) {
            System.out.println("Wait " + wait + "ms " + message);
            Thread.sleep(wait);
        }
    }

    @Scheduled(fixedRate = RATE)
    public void closeOrders() throws InterruptedException {
        if (MarketApplication.webApplicationType == WebApplicationType.NONE) {
            System.out.println("Disabled scheduled due to command line");
            return;
        }
        Iterable<Order> iterableOrder = orderRepository.findOpeningSortedByPurchaseExpires();
        Iterator<Order> iter = iterableOrder.iterator();
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime endSchedule = now.plus(RATE, ChronoUnit.MILLIS);
        while (iter.hasNext()) {
            Order order = iter.next();
            // System.out.println("=== " + order.id);

            ZonedDateTime expires = order.purchaseExpires.atZone(ZoneId.of("UTC"));
            if (expires.isBefore(endSchedule)) { // otherwise, resolve next schedule
                waitToDateTime(expires, "for order " + order.id);
                if (closeOrder.stillOpening(order)) { // status might change during the previous wait
                    System.out.println("Cancel order " + order.buyOrderId);
                    try {
                        api.cancelOrder(order.buyOrderId);
                        closeOrder.discardOpeningOrder(order);
                        Log log = new Log();
                        log.source = "SERVER";
                        log.message = "Http DELETE";
                        log.objectType = "OBJECT";
                        log.object = "\"{\\\"orderId\\\":%d}\"".formatted(order.id);
                        log.createdAt = LocalDateTime.now(ZoneId.of("UTC"));
                        logRepository.save(log);    
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
                }
            } else {
                // Since orders were sorted by purchaseExpires, there is reason to continue
                return;
            }
        }
    }

    @Scheduled(fixedRate = WEBSOCKET_RATE)
    public void ensureIbWebSocketClient() throws InterruptedException {
        if (MarketApplication.webApplicationType == WebApplicationType.NONE) {
            System.out.println("Disabled scheduled due to command line");
            return;
        }
        ibWebSocketClient.ensure();
    }

    @Scheduled(fixedRate = ORDER_RATE)
    public void orderUpdates() throws InterruptedException {
        if (MarketApplication.webApplicationType == WebApplicationType.NONE) {
            System.out.println("Disabled scheduled due to command line");
            return;
        }
        ibOrderScheduler.tick();
    }

}
