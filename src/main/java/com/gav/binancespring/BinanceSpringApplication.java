package com.gav.binancespring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BinanceSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(BinanceSpringApplication.class, args);
    }

}
