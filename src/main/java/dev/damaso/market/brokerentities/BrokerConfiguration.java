package dev.damaso.market.brokerentities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="configuration")
public class BrokerConfiguration {
    @Id
    @Column(name = "\"key\"")
    public String key;
    @Column(name = "\"value\"")
    public String value;
}
