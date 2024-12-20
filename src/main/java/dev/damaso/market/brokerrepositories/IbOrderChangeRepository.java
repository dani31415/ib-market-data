package dev.damaso.market.brokerrepositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.brokerentities.IbOrderChange;

public interface IbOrderChangeRepository extends CrudRepository<IbOrderChange, Integer> {
    @Query("SELECT o FROM IbOrderChange as o WHERE o.ibOrderId=?1 ORDER BY id DESC")
    Iterable<IbOrderChange> findByOrderIdDesc(String ibOrderId);

    @Query("SELECT o FROM IbOrderChange as o WHERE o.ibOrderId=?1 ORDER BY id ASC")
    Iterable<IbOrderChange> findByOrderIdAsc(String ibOrderId);
}
