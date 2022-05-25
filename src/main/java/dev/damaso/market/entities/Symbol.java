package dev.damaso.market.entities;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Symbol {
    @Id
    public int id;

    public String shortName;
}
