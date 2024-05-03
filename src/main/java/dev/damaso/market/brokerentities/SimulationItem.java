package dev.damaso.market.brokerentities;

import java.time.LocalDateTime;

import javax.persistence.Column;
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
    @Column(name = "\"order\"")
    public int order;
    public int period;
    public Integer minute;
    public Integer symbolId;
    public String ib_conid;
    public String symbolSrcName;
    public Float purchase;
    public Float gains;
    public Float early;
    public String modelName;
    public String simulationName;
    public LocalDateTime createdAt;
}
