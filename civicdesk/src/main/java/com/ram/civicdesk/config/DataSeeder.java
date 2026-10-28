package com.ram.civicdesk.config;

import com.ram.civicdesk.service.DemoDataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Creates demo accounts and complaints on first start (when the users table is empty). */
@Component
public class DataSeeder implements ApplicationRunner {

    private final DemoDataService demoDataService;
    private final boolean enabled;

    public DataSeeder(DemoDataService demoDataService, @Value("${app.demo.seed:true}") boolean enabled) {
        this.demoDataService = demoDataService;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (enabled) {
            demoDataService.seedIfEmpty();
        }
    }
}
