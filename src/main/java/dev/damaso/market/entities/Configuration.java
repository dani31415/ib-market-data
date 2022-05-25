package dev.damaso.market.entities;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Configuration {
    @Id
    public String key;

    public String value;      
}
