package com.showup.api;

import com.showup.api.config.TestcontainersConfiguration;
import org.springframework.boot.SpringApplication;

/**
 * Run this instead of {@link ApiApplication} to develop against a disposable Postgres container.
 */
public class TestApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(ApiApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
