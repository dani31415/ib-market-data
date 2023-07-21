package dev.damaso.market.repositories;

import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.entities.Snapshot;
import dev.damaso.market.entities.SnapshotId;

public interface SnapshotRepository extends CrudRepository<Snapshot, SnapshotId> {
}
