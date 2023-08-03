package dev.damaso.market.controllers;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import dev.damaso.market.brokerentities.Replay;
import dev.damaso.market.brokerrepositories.ReplayRepository;

@RestController
public class Replays {
    @Autowired
    ReplayRepository replayRepository;

    @GetMapping("/orders/{orderId}/replay")
    Replay getOrderReplay(@PathVariable Integer orderId) {
        Optional<Replay> optionalReplay = replayRepository.findById(orderId);
        if (!optionalReplay.isPresent()) {
            throw new NotFoundException();
        }
        return optionalReplay.get();
    }

    @PostMapping("/orders/{orderId}/replay")
    void createOrderReply(@PathVariable Integer orderId, @RequestBody Replay reply) {
        reply.orderId = orderId;
        replayRepository.save(reply);
    }
}
