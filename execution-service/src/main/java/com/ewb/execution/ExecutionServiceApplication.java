package com.ewb.execution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class ExecutionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExecutionServiceApplication.class, args);
    }

    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }
}
