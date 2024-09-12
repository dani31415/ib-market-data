package dev.damaso.market.operations;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.damaso.market.brokerentities.IbOrder;
import dev.damaso.market.brokerrepositories.IbOrderRepository;
import dev.damaso.market.external.ibgw.Api;
import dev.damaso.market.external.ibgw.ApiIbOrder;

@Component
public class IbOrderChanges {
    private static Logger logger = LogManager.getLogger(IbOrderChanges.class);

    @Autowired
    public IbOrderRepository ibOrderRepository;

    @Autowired
    public Api api;

    public void tick() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        if (Date.isNasdaqOpen(now.toLocalDateTime())) {
            deactiveOrders();
        }
    }
 
    private void deactiveOrders() {
        List<IbOrder> orders = ibOrderRepository.findAllByActive(true);
        for (IbOrder order : orders) {
            ApiIbOrder apiOrder = api.findOrderById(order.id);
            if (apiOrder.status.equals("Cancelled") || apiOrder.status.equals("Filled") || apiOrder.status.equals("Inactive")) {
                order.active = false;
                order.status = apiOrder.status;
                order.closedAt = LocalDateTime.now(ZoneId.of("UTC"));
                logger.info("deactivate order: " + order.id + " " + apiOrder.status);
                ibOrderRepository.save(order);
            }
        }
    }
}
