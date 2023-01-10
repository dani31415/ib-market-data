package dev.damaso.market.repositories;

import org.springframework.data.repository.PagingAndSortingRepository;

import dev.damaso.market.entities.Log;

public interface LogRepository extends PagingAndSortingRepository<Log, Integer> {
}
