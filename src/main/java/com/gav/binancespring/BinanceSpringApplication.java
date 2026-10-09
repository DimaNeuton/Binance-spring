package com.gav.binancespring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import java.nio.file.Path;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BinanceSpringApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(BinanceSpringApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(BinanceSpringApplication.class, args);
    }

}
