package dev.damaso.market.external.ibgw;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.damaso.market.external.ibgw.implementation.ApiImplementation;

@Configuration
public class ApiConfiguration {
    @Bean
    public Api api() {
        return new ApiImplementation();
    }
}
