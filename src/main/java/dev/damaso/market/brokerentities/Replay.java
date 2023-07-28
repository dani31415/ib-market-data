package dev.damaso.market.brokerentities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Replay {
    @Id
    public int orderId;
    public Integer symbolId;
    public Integer minute;
    public Integer period;
    public String variant;
    public Integer iteration;
    public Integer epoch;

    @Column(columnDefinition="text")
    public String friends;
}
