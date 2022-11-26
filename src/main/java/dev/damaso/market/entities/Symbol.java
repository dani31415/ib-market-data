package dev.damaso.market.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Symbol {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    public String shortName;
    public String ib_conid;
    public String oldNames;
    // public Date firstItemDate;
    // public Date lastItemDate;
}
