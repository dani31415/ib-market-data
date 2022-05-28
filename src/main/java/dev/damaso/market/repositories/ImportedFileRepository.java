package dev.damaso.market.repositories;

import org.springframework.data.repository.CrudRepository;

import dev.damaso.market.entities.ImportedFile;

public interface ImportedFileRepository extends CrudRepository<ImportedFile, String> {
}
