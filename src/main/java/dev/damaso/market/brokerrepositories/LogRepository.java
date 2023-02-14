package dev.damaso.market.brokerrepositories;

import org.springframework.data.repository.PagingAndSortingRepository;

import dev.damaso.market.brokerentities.Log;

public interface LogRepository extends PagingAndSortingRepository<Log, Integer> {
    Iterable<Log> findByMessage(String message);
}
