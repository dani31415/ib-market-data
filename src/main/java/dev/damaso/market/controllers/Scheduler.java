package dev.damaso.market.controllers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dev.damaso.market.CommandLine;
import dev.damaso.market.brokerentities.Log;
import dev.damaso.market.brokerentities.Order;
import dev.damaso.market.brokerrepositories.LogRepository;
import dev.damaso.market.brokerrepositories.OrderRepository;
import dev.damaso.market.external.ibgw.Api;
import dev.damaso.market.operations.CloseOrder;

@Component
public class Scheduler {
    public final int RATE = 60*1000;

    @Autowired
    CloseOrder closeOrder;

    @Autowired
    LogRepository logRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    CommandLine commandLine;

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
        if (commandLine.isCommandLine()) {
            System.out.println("Disabled command line");
            return;
        }
        Iterable<Order> iterableOrder = orderRepository.findOpeningSortedByPurchaseExpires();
        Iterator<Order> iter = iterableOrder.iterator();
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime endSchedule = now.plus(RATE, ChronoUnit.MILLIS);
        while (iter.hasNext()) {
            Order order = iter.next();
            System.out.println("=== " + order.id);

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
}
