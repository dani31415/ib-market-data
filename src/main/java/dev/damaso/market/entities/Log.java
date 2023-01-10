package dev.damaso.market.entities;

import java.time.LocalDateTime;

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
    public String message;
    public String objectType;
    public String object;
    public LocalDateTime createdAt;
}
