package dev.damaso.market.controllers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Iterator;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import dev.damaso.market.brokerentities.IbOrder;
import dev.damaso.market.brokerentities.IbOrderChange;
import dev.damaso.market.brokerrepositories.IbOrderChangeRepository;
import dev.damaso.market.brokerrepositories.IbOrderRepository;

@RestController
public class IbOrders {
    @Autowired
    public IbOrderRepository ibOrderRepository;

    @Autowired
    public IbOrderChangeRepository ibOrderChangeRepository;

    @PutMapping("/iborders")
    @Transactional(transactionManager = "brokerTransactionManager", isolation = Isolation.REPEATABLE_READ)
    public void saveIbOrder(@RequestBody IbOrderSaveRequestDTO orderRequest) throws Exception {
        Optional<IbOrder> result = ibOrderRepository.findById(orderRequest.id);
        IbOrder order;
        if (result.isPresent()) {
            order = result.get();
        } else {
            order = new IbOrder();
            order.createdAt = LocalDateTime.now(ZoneId.of("UTC"));
            order.active = true;
        }
        order.id = orderRequest.id;
        order.orderId = orderRequest.orderId;
        order.side = orderRequest.side;
        order.price = orderRequest.price;
        order.quantity = orderRequest.quantity;
        order.orderRef = orderRequest.orderRef;
        if (orderRequest.status!=null && orderRequest.status.isPresent()) {
            order.status = orderRequest.status.get();
        }
        order.updatedAt = LocalDateTime.now(ZoneId.of("UTC"));

        IbOrderChange ibOrderChange = new IbOrderChange();
        ibOrderChange.ibOrderId = order.id;
        ibOrderChange.price = order.price;
        ibOrderChange.quantity = order.quantity;
        ibOrderChange.status = order.status;
        ibOrderChange.createdAt = order.updatedAt;
        ibOrderChangeRepository.save(ibOrderChange);

        ibOrderRepository.save(order);
    }

    @GetMapping("/iborders/{ibOrderId}")
    @Transactional(transactionManager = "brokerTransactionManager", isolation = Isolation.REPEATABLE_READ)
    public IbSavedOrderDTO saveIbOrder(@PathVariable String ibOrderId) throws Exception {
        Optional<IbOrder> result = ibOrderRepository.findById(ibOrderId);
        if (result.isPresent()) {
            IbOrder ibOrder = result.get();
            IbSavedOrderDTO ibSavedOrder = new IbSavedOrderDTO();
            ibSavedOrder.id = ibOrder.id;
            ibSavedOrder.orderId = ibOrder.orderId;
            ibSavedOrder.active = ibOrder.active;
            ibSavedOrder.orderRef = ibOrder.orderRef;
            ibSavedOrder.price = ibOrder.price;
            ibSavedOrder.quantity = ibOrder.quantity;
            ibSavedOrder.side = ibOrder.side;
            ibSavedOrder.status = ibOrder.status;
            ibSavedOrder.createdAt = ibOrder.createdAt;
            ibSavedOrder.updatedAt = ibOrder.updatedAt;
            ibSavedOrder.closedAt = ibOrder.closedAt;

            Iterable<IbOrderChange> ibOrderChange = ibOrderChangeRepository.findByOrderIdAsc(ibOrder.id);
            Iterator<IbOrderChange> iterator = ibOrderChange.iterator();
            if (iterator.hasNext()) {
                IbOrderChange lastChange = iterator.next();
                ibSavedOrder.originalQuantity = lastChange.quantity;
                ibSavedOrder.originalPrice = lastChange.price;
            }
            return ibSavedOrder;
        }
        return null;
    }
}
