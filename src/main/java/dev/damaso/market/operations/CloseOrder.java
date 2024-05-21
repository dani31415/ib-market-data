package dev.damaso.market.operations;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
// import org.springframework.transaction.support.TransactionSynchronizationManager;

import dev.damaso.market.brokerentities.Order;
import dev.damaso.market.brokerrepositories.OrderRepository;

@Component
public class CloseOrder {
    @Autowired
    OrderRepository orderRepository;

    // Discarded orders might be openend if new transactions arrive
    @Transactional(transactionManager = "brokerTransactionManager", isolation = Isolation.REPEATABLE_READ)
    public void discardOpeningOrder(Order order) {
        Optional<Order> optionalUpdatedOrder = orderRepository.findById(order.id);
        if (optionalUpdatedOrder.isPresent()) {
            Order updatedOrder = optionalUpdatedOrder.get();
            // Discard it only if it is still opening
            if (updatedOrder.status.equals("opening")) {
                updatedOrder.status = "discarded";
                // if (TransactionSynchronizationManager.isActualTransactionActive()) {
                //     System.out.println("Discard order under transacction.");
                // }
                orderRepository.save(updatedOrder);
            }
        }
    }

    @Transactional(transactionManager = "brokerTransactionManager", isolation = Isolation.REPEATABLE_READ)
    public boolean stillOpening(Order order) {
        Optional<Order> optionalUpdatedOrder = orderRepository.findById(order.id);
        if (optionalUpdatedOrder.isPresent()) {
            Order updatedOrder = optionalUpdatedOrder.get();
            if (updatedOrder.status.equals("opening")) {
                return true;
            }
        }
        return false;
    }

}
