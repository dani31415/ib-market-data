package dev.damaso.market.controllers;

import java.time.LocalDateTime;

public class LogRequestDTO {
    public String source;
    public String message;
    public String objectType;
    public String object;
    public LocalDateTime createdAt;
}
