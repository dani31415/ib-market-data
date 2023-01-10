package dev.damaso.market.controllers;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.damaso.market.entities.Log;
import dev.damaso.market.repositories.LogRepository;

@RestController
public class LogController {
    public final int PAGE_SIZE = 100;
    @Autowired
    LogRepository logRepository;
    
    @PostMapping("/logs")
    public void addLog(@RequestBody LogRequestDTO logRequestDTO) throws Exception {
        Log log = new Log();
        log.message = logRequestDTO.message;
        log.source = logRequestDTO.source;
        log.objectType = logRequestDTO.objectType;
        log.object = logRequestDTO.object;
        log.createdAt = LocalDateTime.now(ZoneId.of("UTC"));
        this.logRepository.save(log);
    }

    @GetMapping("/logs")
    public Iterable<Log> getLogs(@RequestParam(required=false, defaultValue="0") int page) throws Exception {
        Pageable pageable = PageRequest.of(page * this.PAGE_SIZE, this.PAGE_SIZE, Sort.by("id").descending());
        return logRepository.findAll(pageable).getContent();
    }
}
