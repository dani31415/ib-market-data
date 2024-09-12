package dev.damaso.market.operations;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.damaso.market.brokerentities.IbOrder;
import dev.damaso.market.brokerrepositories.IbOrderRepository;
import dev.damaso.market.external.ibgw.Api;
import dev.damaso.market.external.ibgw.ApiIbOrder;

@Component
public class IbOrderChanges {
    @Autowired
    public IbOrderRepository ibOrderRepository;

    @Autowired
    public IbOrderChanged ibOrderChanged;

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
            ibOrderChanged.changed(order.id, apiOrder);
        }
    }
}
