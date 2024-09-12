package dev.damaso.market.operations;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import dev.damaso.market.brokerentities.IbOrder;
import dev.damaso.market.brokerrepositories.IbOrderRepository;
import dev.damaso.market.external.ibgw.Api;
import dev.damaso.market.external.ibgw.ApiIbOrder;

@Component
public class IbOrderChanged {
    private static Logger logger = LogManager.getLogger(IbOrderChanged.class);

    @Autowired
    public IbOrderRepository ibOrderRepository;

    @Autowired
    public Api api;

    @Transactional(transactionManager = "brokerTransactionManager", isolation = Isolation.REPEATABLE_READ)
    public void changed(String id) {
        Optional<IbOrder> optional = ibOrderRepository.findById(id);
        if (!optional.isPresent()) {
            return;
        }

        IbOrder order = optional.get();
        if (!order.active) {
            return;
        }

        ApiIbOrder apiOrder = api.findOrderById(id);
        if (apiOrder.status.equals("Cancelled") || apiOrder.status.equals("Filled") || apiOrder.status.equals("Inactive")) {
            order.active = false;
            order.status = apiOrder.status;
            order.closedAt = LocalDateTime.now(ZoneId.of("UTC"));
            logger.info("deactivate order: " + order.id + " " + apiOrder.status);
            ibOrderRepository.save(order);
        }
    }    
}
