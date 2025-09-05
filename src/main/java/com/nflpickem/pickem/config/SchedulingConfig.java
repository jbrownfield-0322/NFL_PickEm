package com.nflpickem.pickem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulingConfig {
    // This class enables Spring's scheduling functionality
    // The @EnableScheduling annotation allows @Scheduled methods to work
}
