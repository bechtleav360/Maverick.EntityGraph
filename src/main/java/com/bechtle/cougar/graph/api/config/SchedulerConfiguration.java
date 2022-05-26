package com.bechtle.cougar.graph.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
//@Profile({"prod", "stage", "it", "persistent"})
public class SchedulerConfiguration {
}
