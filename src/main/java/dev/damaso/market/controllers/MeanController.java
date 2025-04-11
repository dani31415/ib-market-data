package dev.damaso.market.controllers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.damaso.market.brokerentities.Mean;
import dev.damaso.market.brokerentities.MeanId;
import dev.damaso.market.brokerrepositories.MeanRepository;
import dev.damaso.market.entities.Period;
import dev.damaso.market.repositories.PeriodRepository;

@RestController
public class MeanController {
    @Autowired
    MeanRepository meanRepository;

    @Autowired
    PeriodRepository periodRepository;

    @PostMapping("/means")
    public boolean createSimulationItem(@RequestBody Mean meanRequest) throws Exception {
        Optional<Period> optionalPeriod = periodRepository.findById(meanRequest.period);
        if (!optionalPeriod.isPresent()) {
            meanRequest.date = optionalPeriod.get().date;
        }
        meanRepository.save(meanRequest);
        return true;
    }

    @GetMapping("/means")
    public Iterable<Mean> getSimulationItems(@RequestParam(required=false) Integer period, @RequestParam(required=false) String date, @RequestParam(required=false) String modelName) throws Exception {
        if (period==null && date==null && modelName==null) {
            return meanRepository.findAll();
        }
        if (date != null) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate localDate = LocalDate.parse(date, dtf);
            Optional<Period> optionalPeriod = periodRepository.findByDate(localDate);
            if (!optionalPeriod.isPresent()) {
                throw new Error("Wrong date");
            }
            period = optionalPeriod.get().id;
        }

        MeanId meanId = new MeanId();
        meanId.period = period;
        meanId.modelName = modelName;
        List<Mean> list = new ArrayList<>();
        Optional<Mean> optionalMean = meanRepository.findById(meanId);
        if (!optionalMean.isPresent()) {
            list.add(optionalMean.get());
        }
        return list;
    }
}
