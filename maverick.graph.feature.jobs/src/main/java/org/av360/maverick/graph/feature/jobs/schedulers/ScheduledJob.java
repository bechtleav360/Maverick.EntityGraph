package org.av360.maverick.graph.feature.jobs.schedulers;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashSet;
import java.util.Set;

@Slf4j(topic = "graph.jobs")
public class ScheduledJob {

    private final Set<String> active;

    public ScheduledJob() {
        active = new HashSet<>();
    }

    // see https://github.com/spring-projects/spring-framework/pull/29924
    protected void schedule(Mono<Void> job, String label) {
        if(this.active.contains(label)) {
            log.debug("Job '{}' still running, skipping this scheduled run.", label);
            return;
        }

        job
                .doOnSubscribe(subscription -> {
                    log.info("Starting job '{}'", label);
                    this.active.add(label);
                })
                .doOnError(error -> {
                    log.warn("Failed job '{}'", label);
                    this.active.remove(label);
                })
                .doOnSuccess(success -> {
                    log.debug("Completed job '{}'", label);
                    this.active.remove(label);
                })
                .publishOn(Schedulers.boundedElastic())
                .subscribe();



    }
}
