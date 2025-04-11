package dev.damaso.market.brokerentities;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

@Entity
@IdClass(MeanId.class)
public class Mean {
    @Id
    public int period;
    @Id
    public String modelName;
    public Float mean;
}
