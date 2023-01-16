package dev.damaso.market.brokerentities;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Log {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    public String source;
    @Column(columnDefinition="text")
    public String message;
    public String objectType;
    @Column(columnDefinition="mediumtext")
    public String object;
    public LocalDateTime createdAt;
}
