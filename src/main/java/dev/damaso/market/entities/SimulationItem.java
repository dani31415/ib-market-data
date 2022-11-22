package dev.damaso.market.entities;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class SimulationItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    public String groupGuid;
    public int order;
    public int period;
    public int symbolId;
    public String ib_conid;
    public String symbolSrcName;
    public float openPrice;
    public float gain;
    public String modelName;
    public LocalDateTime createdAt;
}
