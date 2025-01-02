package dev.damaso.market.controllers;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import dev.damaso.market.brokerentities.BrokerConfiguration;
import dev.damaso.market.brokerrepositories.BrokerConfigurationRepository;

@RestController
public class ConfigurationController {
    @Autowired
    BrokerConfigurationRepository configurationRepository;

    @GetMapping("/configurations/{key}")
	public String getConfiguration(@PathVariable String key) {
        Optional<BrokerConfiguration> value = configurationRepository.findById(key);
        if (value.isPresent()) {
            return value.get().value;
        }
        return "";
    }

    @PutMapping("/configurations/{key}")
	public void setConfiguration(@PathVariable String key, @RequestBody ConfigurationDTO configurationDTO) {
        BrokerConfiguration configuration = new BrokerConfiguration();
        configuration.key = key;
        configuration.value = configurationDTO.value;
        configurationRepository.save(configuration);
    }
}
