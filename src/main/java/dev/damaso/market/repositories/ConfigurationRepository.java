package dev.damaso.market.repositories;

import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.entities.Configuration;

public interface ConfigurationRepository extends CrudRepository<Configuration, String> {
    
}
