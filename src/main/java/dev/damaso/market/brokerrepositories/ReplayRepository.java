package dev.damaso.market.brokerrepositories;

import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.brokerentities.Replay;

public interface ReplayRepository extends CrudRepository<Replay, Integer> {
}
