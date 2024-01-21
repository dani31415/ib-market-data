package dev.damaso.market.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.entities.UpdateLog;

public interface UpdateLogRepository extends CrudRepository<UpdateLog, Integer> {
    @Query("SELECT ul FROM UpdateLog ul WHERE ul.name = ?1 AND ul.datetime >= ?2")
    public List<UpdateLog> updateExists(String name, LocalDateTime datetime);
}
