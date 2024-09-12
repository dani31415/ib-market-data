package dev.damaso.market.brokerrepositories;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.brokerentities.IbOrder;

public interface IbOrderRepository extends CrudRepository<IbOrder, String> {   
    List<IbOrder> findAllByActive(boolean active);
}
