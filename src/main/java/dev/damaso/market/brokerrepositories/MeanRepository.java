package dev.damaso.market.brokerrepositories;

import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.brokerentities.Mean;
import dev.damaso.market.brokerentities.MeanId;

public interface MeanRepository extends CrudRepository<Mean, MeanId> {
}
