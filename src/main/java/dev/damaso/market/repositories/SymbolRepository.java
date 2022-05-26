package dev.damaso.market.repositories;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.entities.Symbol;

public interface SymbolRepository extends CrudRepository<Symbol, Integer> {
    @Cacheable("symbols")
    Symbol findSymbolByShortName(String shortName);
}
