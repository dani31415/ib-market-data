package dev.damaso.market.brokerrepositories;

import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.brokerentities.IbOrderChange;

public interface IbOrderChangeRepository extends CrudRepository<IbOrderChange, Integer> {   
}
