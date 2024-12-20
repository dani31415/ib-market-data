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
import dev.damaso.market.brokerentities.IbOrderChange;
import dev.damaso.market.brokerrepositories.IbOrderChangeRepository;
import dev.damaso.market.brokerrepositories.IbOrderRepository;

@Component
public class IbOrderChanged {
    private static Logger logger = LogManager.getLogger(IbOrderChanged.class);

    @Autowired
    public IbOrderRepository ibOrderRepository;

    @Autowired
    public IbOrderChangeRepository ibOrderChangeRepository;

    @Autowired
    public Jenkins jenkins;

    @Transactional(transactionManager = "brokerTransactionManager", isolation = Isolation.REPEATABLE_READ)
    public PostActions changed(String id, IbOrderChanges changes) {
        Optional<IbOrder> optional = ibOrderRepository.findById(id);
        PostActions postActions = new PostActions();
        if (!optional.isPresent()) {
            return postActions;
        }

        IbOrder order = optional.get();
        if (!order.active) {
            return postActions;
        }

        if (changes.quantity !=null && !floatEquals(changes.quantity, order.quantity)) {
            logger.info("order quantity changed: " + order.id + " " + order.quantity + " --> " + changes.quantity);
            order.quantity = changes.quantity;
            order.updatedAt = LocalDateTime.now(ZoneId.of("UTC"));
            ibOrderRepository.save(order);

            IbOrderChange ibOrderChange = new IbOrderChange();
            ibOrderChange.ibOrderId = order.id;
            ibOrderChange.price = order.price;
            ibOrderChange.quantity = order.quantity;
            ibOrderChange.createdAt = order.updatedAt;
            ibOrderChangeRepository.save(ibOrderChange);

            postActions.setJenkins(jenkins);
            postActions.notifyJenkins = order.id;
        }

        if (changes.status.equals("Cancelled") || changes.status.equals("Filled") || changes.status.equals("Inactive")) {
            order.active = false;
            order.status = changes.status;
            order.closedAt = LocalDateTime.now(ZoneId.of("UTC"));
            logger.info("deactivate order: " + order.id + " " + changes.status);
            ibOrderRepository.save(order);
        }
        return postActions;
    }

    private boolean floatEquals(float x, float y) {
        return Math.abs(x - y) < 1e-5;
    }
}
