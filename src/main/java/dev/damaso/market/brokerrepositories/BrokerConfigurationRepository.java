package dev.damaso.market.brokerrepositories;

import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.brokerentities.BrokerConfiguration;

public interface BrokerConfigurationRepository extends CrudRepository<BrokerConfiguration, String> {
}
