package dev.damaso.market.entities;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class ImportedFile {
    @Id
    public String fileName;
}
