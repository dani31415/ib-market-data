package dev.damaso.market.entities;

import java.time.LocalDate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Period {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    public LocalDate date;
    public Float mean;
    public boolean updated;

    public int getOrder() {
        return id-1;
    }
}
