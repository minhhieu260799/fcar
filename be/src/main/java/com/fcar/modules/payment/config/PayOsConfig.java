package com.fcar.modules.payment.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Phân công: Minh — bean RestClient PayOS. */
@Configuration
@EnableConfigurationProperties(FcarPayOsProperties.class)
public class PayOsConfig {

    @Bean
    public RestClient payOsRestClient(FcarPayOsProperties props) {
        return RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .build();
    }
}
