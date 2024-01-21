package dev.damaso.market.controllers;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.damaso.market.repositories.UpdateLogRepository;

@RestController
public class UpdateLog {
    @Autowired
    UpdateLogRepository updateLogRepository;

    @GetMapping("/updatelog/exists")
    boolean getSizeByOrderRef(@RequestParam String name, @RequestParam String datetime) {
        // Used during testing
        LocalDateTime datetimeObj = LocalDateTime.parse(datetime);
        return updateLogRepository.updateExists(name, datetimeObj).size()>0;
    }
}
